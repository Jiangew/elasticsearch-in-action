# Elasticsearch in Action

## Rest Client

## High Level Rest Client

## Elasticsearch IK 分词器
### IK 带有两个分词器
* ik_max_word：会将文本做最细粒度的拆分；尽可能多的拆分出词语
* ik_smart：会做最粗粒度的拆分；已被分出的词语将不会再次被其它词语占有

#### ik_max_word
```sh
curl -XGET 'http://localhost:9200/_analyze?pretty&analyzer=ik_max_word' -d '联想是全球最大的笔记本厂商'
curl -XGET 'http://localhost:9200/_analyze?pretty&analyzer=ik_max_word' -d '希拉里和韩国'
```
```json
{
  "tokens" : [
    {
      "token" : "联想",
      "start_offset" : 0,
      "end_offset" : 2,
      "type" : "CN_WORD",
      "position" : 0
    },
    {
      "token" : "是",
      "start_offset" : 2,
      "end_offset" : 3,
      "type" : "CN_CHAR",
      "position" : 1
    },
    {
      "token" : "全球",
      "start_offset" : 3,
      "end_offset" : 5,
      "type" : "CN_WORD",
      "position" : 2
    },
    {
      "token" : "最大",
      "start_offset" : 5,
      "end_offset" : 7,
      "type" : "CN_WORD",
      "position" : 3
    },
    {
      "token" : "的",
      "start_offset" : 7,
      "end_offset" : 8,
      "type" : "CN_CHAR",
      "position" : 4
    },
    {
      "token" : "笔记本",
      "start_offset" : 8,
      "end_offset" : 11,
      "type" : "CN_WORD",
      "position" : 5
    },
    {
      "token" : "笔记",
      "start_offset" : 8,
      "end_offset" : 10,
      "type" : "CN_WORD",
      "position" : 6
    },
    {
      "token" : "本厂",
      "start_offset" : 10,
      "end_offset" : 12,
      "type" : "CN_WORD",
      "position" : 7
    },
    {
      "token" : "厂商",
      "start_offset" : 11,
      "end_offset" : 13,
      "type" : "CN_WORD",
      "position" : 8
    }
  ]
}
```

#### ik_smart
```sh
curl -XGET 'http://localhost:9200/_analyze?pretty&analyzer=ik_smart' -d '联想是全球最大的笔记本厂商'
```
```json
{
  "tokens" : [
    {
      "token" : "联想",
      "start_offset" : 0,
      "end_offset" : 2,
      "type" : "CN_WORD",
      "position" : 0
    },
    {
      "token" : "是",
      "start_offset" : 2,
      "end_offset" : 3,
      "type" : "CN_CHAR",
      "position" : 1
    },
    {
      "token" : "全球",
      "start_offset" : 3,
      "end_offset" : 5,
      "type" : "CN_WORD",
      "position" : 2
    },
    {
      "token" : "最大",
      "start_offset" : 5,
      "end_offset" : 7,
      "type" : "CN_WORD",
      "position" : 3
    },
    {
      "token" : "的",
      "start_offset" : 7,
      "end_offset" : 8,
      "type" : "CN_CHAR",
      "position" : 4
    },
    {
      "token" : "笔记本",
      "start_offset" : 8,
      "end_offset" : 11,
      "type" : "CN_WORD",
      "position" : 5
    },
    {
      "token" : "厂商",
      "start_offset" : 11,
      "end_offset" : 13,
      "type" : "CN_WORD",
      "position" : 6
    }
  ]
}
```

### 创建索引 newsik，设置分析器 ik，指定分词器 ik_max_word；创建类型 article，字段 subject
```sh
curl -XPUT 'http://localhost:9200/newsik?pretty' -d '{
    "settings" : {
        "analysis" : {
            "analyzer" : {
                "ik" : {
                    "tokenizer" : "ik_max_word"
                }
            }
        }
    },
    "mappings" : {
        "article" : {
            "dynamic" : true,
            "properties" : {
                "subject" : {
                    "type" : "string",
                    "analyzer" : "ik_max_word"
                }
            }
        }
    }
}'
```

### 批量添加记录，指定元数据 _id 方便查询
```sh
curl -XPOST http://localhost:9200/newsik/article/_bulk?pretty -d '
{ "index" : { "_id" : "1" } }
{"subject" : "＂闺蜜＂崔顺实被韩检方传唤 韩总统府促彻查真相" }
{ "index" : { "_id" : "2" } }
{"subject" : "韩举行＂护国训练＂ 青瓦台:决不许国家安全出问题" }
{ "index" : { "_id" : "3" } }
{"subject" : "媒体称FBI已经取得搜查令 检视希拉里电邮" }
{ "index" : { "_id" : "4" } }
{"subject" : "村上春树获安徒生奖 演讲中谈及欧洲排外问题" }
{ "index" : { "_id" : "5" } }
{"subject" : "希拉里团队炮轰FBI 参院民主党领袖批其“违法”" }
'
```

### 查询 "希拉里和韩国"；设置高亮属性 highlight，直接显示到 html 中，被匹配到的字或词将以红色突出显示；若要用过滤搜索，直接将 match 改为 term 即可
```sh
curl -XPOST http://localhost:9200/newsik/article/_search?pretty -d '
{
    "query" : { "match" : { "subject" : "希拉里和韩国" }},
    "highlight" : {
        "pre_tags" : ["<font color='red'>"],
        "post_tags" : ["</font>"],
        "fields" : {
            "subject" : {}
        }
    }
}
'
```
```json
{
  "took" : 113,
  "timed_out" : false,
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "failed" : 0
  },
  "hits" : {
    "total" : 4,
    "max_score" : 0.034062363,
    "hits" : [ {
      "_index" : "newsik",
      "_type" : "article",
      "_id" : "2",
      "_score" : 0.034062363,
      "_source" : {
        "subject" : "韩举行＂护国训练＂ 青瓦台:决不许国家安全出问题"
      },
      "highlight" : {
        "subject" : [ "<font color=red>韩</font>举行＂护<font color=red>国</font>训练＂ 青瓦台:决不许国家安全出问题" ]
      }
    }, {
      "_index" : "newsik",
      "_type" : "article",
      "_id" : "3",
      "_score" : 0.0076681254,
      "_source" : {
        "subject" : "媒体称FBI已经取得搜查令 检视希拉里电邮"
      },
      "highlight" : {
        "subject" : [ "媒体称FBI已经取得搜查令 检视<font color=red>希拉里</font>电邮" ]
      }
    }, {
      "_index" : "newsik",
      "_type" : "article",
      "_id" : "5",
      "_score" : 0.006709609,
      "_source" : {
        "subject" : "希拉里团队炮轰FBI 参院民主党领袖批其“违法”"
      },
      "highlight" : {
        "subject" : [ "<font color=red>希拉里</font>团队炮轰FBI 参院民主党领袖批其“违法”" ]
      }
    }, {
      "_index" : "newsik",
      "_type" : "article",
      "_id" : "1",
      "_score" : 0.0021509775,
      "_source" : {
        "subject" : "＂闺蜜＂崔顺实被韩检方传唤 韩总统府促彻查真相"
      },
      "highlight" : {
        "subject" : [ "＂闺蜜＂崔顺实被<font color=red>韩</font>检方传唤 <font color=red>韩</font>总统府促彻查真相" ]
      }
    } ]
  }
}
```

### 热词更新配置
#### 测试下，IK主词典不包含"陈港生"这个词，所以被拆分了
```sh
curl -XGET 'http://localhost:9200/_analyze?pretty&analyzer=ik_max_word' -d '成龙原名陈港生'
curl -XGET 'http://localhost:9200/_analyze?pretty&analyzer=ik_max_word' -d '蓝瘦香菇'
```
#### IK 词典配置
* 修改 IK 的配置文件：elasticsearch/plugins/ik/config/ik/IKAnalyzer.cfg.xml
* 远程词典 或 本地词典
* IK 接收两个返回的头部属性 Last-Modified 和 ETag，只要其中一个有变化，就会触发更新，IK 会每分钟获取一次

#### 词典格式
```sh
$s = <<<'EOF'
陈港生
元楼
蓝瘦
EOF;
header('Last-Modified: '.gmdate('D, d M Y H:i:s', time()).' GMT', true, 200);
header('ETag: "5816f349-19"');
echo $s;
```
