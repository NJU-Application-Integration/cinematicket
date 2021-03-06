# 电影票信息整合系统

本系统整合了美团，格瓦拉，百度糯米的电影票信息，用户可以查看到南京市各影院的电影信息。

系统演示地址:http://www.alandelip.cn:8081/Movie/index.html

Github地址:https://github.com/NJU-Application-Integration/cinematicket.

## 概述

数据的爬取和处理是离线的，整个流程分为两步：

1. 爬取各网站的数据，转化为XML文档，对应爬虫部分。
2. 分析XML文档，处理异构信息，存入数据库，对应数据整合部分。

其中面临的数据异构问题主要有

1. 基本类型的格式，如日期的表示格式，null值的表示形式。
2. 电影名异构，如《李雷和韩梅梅：昨日重现》和《李雷和韩梅梅——昨日重现》。
3. 影院名异构，如`幸福蓝海IMAX影城（常发广场店）`和`幸福蓝海国际影城(南京常发IMAX店)`。

其中1主要由爬虫部分解决，2和3由数据整合部分解决。


## 爬虫部分

爬虫部分主要功能是将不同网页中的数据转化为格式基本一致的XML。语言为Python3，框架是Scrapy。针对美团，糯米，格瓦拉三个网站，有三个具体的爬虫实现。

爬虫的主要思路是逐层爬取，具体步骤是：

1.	爬取正在上映的电影列表
2.	逐个爬取电影的上映日期
3.	逐个获取每个日期的各个影院
4.	逐个获取该影院当天的排片表

代码表示为
```python
def start_requests(self):
	return [scrapy.FormRequest(start_url, callback=self.parse_movie)]
def parse_movie(self, response):
	# dosomething
	for next_request in hrefs:
		yield scrapy.Request(next_request, callback=self.parse_date)
def parse_date(self, response):
	# dosomething
	for next_request in hrefs:
		yield scrapy.Request(next_request, callback=self.parse_cinema)
def parse_cinema(self, response):
	# dosomething
	for next_request in hrefs:
		yield scrapy.Request(next_request, callback=self.parse_ajax)
def parse_ajax(self, response):
	# dosomething
	item = FilmItem(name, cinema, date, screenings)
	return item
```

最后的XML数据格式为

```XML
<items>
	<item>
		<name>电影名</name>
		<cinema>影院名</cinema>
		<date>日期</date>
		<screenings>
			<value>
				<auditorium>放映厅名</auditorium>
				<time>开始时间</time>
				<price>价格，美团的因为反爬机制爬不到这一项</price>
				<remain>剩余座位百分比，只有糯米的有</remain>
				<language>语言，只有格瓦拉的有</language>
				<type>语言+是否3D，只有美团的有</type>
			</value>
		</screenings>
	</item>
</items>
```

一些细节问题的处理:

1.	爬取时处理部分异构问题，如日期格式，浮点数精度和null值
1.	为了防止IP被禁用并兼顾速度，设置了每次请求下载间隔为150ms。
1.	并行爬取，减少阻塞。
1.	修改cookie来伪造登录状态与当前所在城市（南京）。
1.	修改agent伪造为浏览器访问，防止被拦截。


## 数据整合部分

数据整合部分的主要功能是根据各网站的XML文档，将数据整合，存入数据库以便应用程序使用。

### 数据设计
有三个Entity，分别为 

* Movie（代表一部电影，有名称，评分等属性）
* Cinema（代表一家影院）
* Screen （代表一个场次，由电影id，影院id，日期，时间唯一确定）

### 整合的过程

首先初始化电影的表，其中要解决影片名不一致的问题。我们首先去除电影名中的标点符号和空格，如将`李雷和韩梅梅：昨日重现`变为`李雷和韩梅梅昨日重现`，这样绝大多数的异构问题就解决了。

但有一些个例，如`潜艇总动员5时光宝盒`和`潜艇总动员之时光宝盒`，即使去掉标点也仍不相同。针对这类影片名，我们建立了同义词表来判断它是否相同。

> 注意，这里不能使用文本的相似度分析算法，如《银河护卫队》和《银河守卫队》是不同的电影，但文本非常相似。也不能去掉电影名中的数字，因为存在《29+1》或《1942》这类电影名。

之后要初始化影院表，需要解决影院名不同的问题。我们观察到美团的影院名非常规整，如`幸福蓝海国际影城（马群店）`，`星河国际影城（江宁店）`，等，都为`影院名（分店名）`的格式。因此我们采取如下做法：
    
1. 首先分析美团的影院数据，去掉其中如“国际”，“影院”，“影城”之类的无关词，然后提取出影院名和分店名，如`幸福蓝海国际影城（马群店）`被转化为（幸福蓝海，马群）。
    
2. 分析其他影院名，若同时包含某个影院的影院名和分店名，则视为同一家影院。如`幸福蓝海影城南京马群店`包含了 （幸福蓝海，马群），则视为同一家影城。

>对于没有分店名的影院,如新街口国际影城,则只需要包含影院名就视为同一家。

很遗憾，这里采用文本相似度分析的算法效果不佳。我们试着将影院名分词，然后计算两组词的余弦相似度，由于文本太短，会出现较多误判情况。

最后需要初始化场次数据，不同的网站包含了许多场次信息，很多场次是重复的。我们使用（影院id，电影id，日期，时间）唯一确定一个场次。
