# Elasticsearch in Action

## 使用「Filters」优化查询
ElasticSearch 查询DSL允许用户使用的绝大多数查询都会有各自的标识，这些查询也以嵌套到如下的查询类型中：
* constant_score
* filtered
* custom_filters_score

为什么要这么麻烦来使用 filtering ？在什么场景下可以只使用 queries ？

### 过滤器「Filters」和缓存
filters来做缓存是一个很不错的选择，ElasticSearch也提供了这种特殊的缓存，filter cache来存储filters得到的结果集。此外，缓存filters不需要太多的内存（它只保留一种信息，即哪些文档与filter相匹配），同时它可以由其它的查询复用，极大地提升了查询的性能。

设想你正在运行如下的查询：
```json
{
     "query": {
          "bool": {
               "must": [
                    { "term": { "name": "joe" } },
                    { "term": { "year": 1981 } }
               ]
          }
     }
}
```
该命令会查询到满足如下条件的文档：name域值为joe，同时year域值为1981。

如果用上面命令的格式构建查询，查询对象会将所有的条件绑定到一起存储到缓存中；因此如果我们查询人名相同但是出生年份不同的运动员，ES无法重用上面查询命令中的任何信息。因此，我们来试着优化一下查询。由于一千个人可能会有一千个人名，所以人名不太适合缓存起来；但是年份比较适合。因此我们引入一个不同的查询命令，将一个简单的query与一个filter结合起来。
```json
{
     "query": {
          "filtered": {
               "query": {
                    "term": { "name": "joe" }
               },
               "filter": {
                    "term": { "year": 1981 }
               }
          }
     }
}
```
我们使用了一个filtered类型的查询对象，查询对象将query元素和filter元素都包含进去了。第一次运行该查询命令后，ES就会把filter缓存起来，如果再有查询用到了一样的filter，就会直接用到缓存。就这样，ES不必多次加载同样的信息。

### 并非所有的「Filters」都会被默认缓存
缓存很强大，但实际上ES在默认情况下并不会缓存所有的filters。这是因为部分filters会用到域数据缓存「field data cache」。该缓存一般用于按域值排序和faceting操作的场景中。默认情况下，如下的filters不会被缓存：
* numeric_range
* script
* geo_bbox
* geo_distance
* geo_distance_range
* geo_polygon
* geo_shape
* and
* or
* not

尽管上面提到的最后三种filters不会用到域缓存，它们主要用于控制其它的filters，因此它不会被缓存，但是它们控制的filters在用到的时候都已经缓存好了。

### 更改 ElasticSearch 缓存的行为
ElasticSearch允许用户通过使用「_chache」和「_cache_key」属性自行开启或关闭filters的缓存功能。

回到前面的例子，假定我们将关键词过滤器的结果缓存起来，并给缓存项的key取名为「year_1981_cache」，则查询命令如下：
```json
{
     "query": {
          "filtered": {
               "query": {
                    "term": { "name": "joe" }
               },
               "filter": {
                    "term": { 
                        "year": 1981,
                        "_cache_key": "year_1981_cache"
                        }
               }
          }
     }
}
```

也可以使用如下的命令关闭该关键词过滤器的缓存：
```json
{
     "query": {
          "filtered": {
               "query": {
                    "term": { "name": "joe" }
               },
               "filter": {
                    "term": { 
                        "year": 1981,
                        "_cache": false
                        }
               }
          }
     }
}
```

### 为什么要这么麻烦的给缓存项的Key命名
我们是否有必要如此麻烦地使用「_cache_key」属性，ElasticSearch不能自己实现这个功能吗？当然可以自己实现，而且在必要的时候控制缓存，但是有时我们需要更多的控制权。

比如，有些查询复用的机会不多，我们希望定时清除这些查询的缓存。如果不指定「_cache_key」，那就只能清除整个过滤器缓存(filter cache)；反之，只需要执行如下的命令即可清除特定的缓存：
```sh
curl -XPOST 'localhost:9200/users/_cache/clear?filter_keys=year_1981_cache'
```

### 什么时候应该改变 ElasticSearch 过滤器缓存的行为
很多时候应该更多去了解业务需求，而不是让ElasticSearch来预测数据分布。

比如，假设你想使用「geo_distance」过滤器将查询限制到有限的几个地理位置，该过滤器在很多查询请求中都使用着相同的参数值，即同一个脚本会随着过滤器一起多次使用。在这个场景中，为过滤器开启缓存是值得的。任何时候都需要问自己这个问题"过滤器会被多次重复使用吗?"。添加数据到缓存是个消耗机器资源的操作，用户应避免不必要的资源浪费。

### 关键词查找过滤器
随着ElasticSearch 0.90版本的发布，我们得到了一个精巧的过滤器，它可以用来将多个从ElasticSearch中得到值作为query的参数「类似于SQL的IN操作」。

让我们看一个简单的例子。假定我们有在一个在线书店，存储了用户，即书店的顾客购买的书籍信息。books索引很简单：
```sh
curl -XPUT 'http://localhost:9200/books?pretty' -d '{
     "mappings": {
          "book": {
               "properties": {
                    "id": {
                         "type": "string",
                         "store": "yes",
                         "index": "not_analyzed"
                    },
                    "title": {
                         "type": "string",
                         "store": "yes",
                         "index": "analyzed"
                    }
               }
          }
     }
}'
```

上面的代码中，没有什么是非同寻常的；只有书籍的id和标题。 接下来，我们来看看用户信息orders索引的mappings信息：
```sh
curl -XPUT 'http://localhost:9200/orders?pretty' -d '{
     "mappings": {
          "order": {
               "properties": {
                    "id": {
                         "type": "string",
                         "store": "yes",
                         "index": "not_analyzed"
                    },
                    "name": {
                         "type": "string",
                         "store": "yes",
                         "index": "analyzed"
                    },
                    "books": {
                         "type": "string",
                         "store": "yes",
                         "index": "not_analyzed"
                    }
               }
          }
     }
}'
```
索引定义了id信息，名字，用户购买书籍的id列表。

写入一些测试数据：
```sh
curl -XPUT 'arslan.bookcs.3g.qq.com/orders/order/1?pretty' -d '{
 "id":"1", "name":"Joe Doe", "books":["1","3"]
}'
curl -XPUT 'arslan.bookcs.3g.qq.com/orders/order/2?pretty' -d '{
 "id":"2", "name":"Jane Doe", "books":["3"]
}'
curl -XPUT 'arslan.bookcs.3g.qq.com/books/book/1?pretty' -d '{
 "id":"1", "title":"Test book one"
}'
curl -XPUT 'arslan.bookcs.3g.qq.com/books/book/2?pretty' -d '{
 "id":"2", "title":"Test book two"
}'
curl -XPUT 'arslan.bookcs.3g.qq.com/books/book/3?pretty' -d '{
 "id":"3", "title":"Test book three"
}'
```

需求如下：我们希望展示某个用户购买的所有书籍。以id为1的user为例。

当然，我们可以先执行一个请求得到当前顾客的购买记录，然后把books域中的值取出来，执行第二个查询。
```sh
curl -XGET 'arslan.bookcs.3g.qq.com/orders/order/1?pretty'
```
```sh
curl -XGET 'arslan.bookcs.3g.qq.com/books/_search?pretty' -d '{
"query" : {
        "ids" : {
            "type" : "book",
            "values" : [ "1", "3" ]
        }
    }
}'
```

这样做太麻烦了，ElasticSearch 0.90版本新引入了关键词查询过滤器「term lookup filter」，该过滤器只需要一个查询就可以将上面两个查询才能完成的事情搞定。使用该过滤器的查询如下：
```sh
curl -XGET 'arslan.bookcs.3g.qq.com/books/_search?pretty' -d '{
    "query" : {
        "filtered" : {
            "query" : {
                "match_all" : {}
            },
            "filter" : {
                "terms" : {
                    "id" : {
                        "index" : "orders",
                        "type" : "order",
                        "id" : "1",
                        "path" : "books"
                    },
                    "_cache_key" : "terms_lookup_order_1_books"
                }
            }
        }
    }
}'
```
Error: "no [query] registered for [filtered]"

Fixed: The [filtered query] has been deprecated and removed in ES 5.0. You should now use the [bool/must/filter query] instead.
```sh
curl -XGET 'arslan.bookcs.3g.qq.com/books/_search?pretty' -d '{
    "query" : {
        "bool" : {
            "must" : {
                "match_all" : {}
            },
            "filter" : {
                "terms" : {
                    "id" : {
                        "index" : "orders",
                        "type" : "order",
                        "id" : "1",
                        "path" : "books"
                    },
                    "_cache_key" : "terms_lookup_order_1_books"
                }
            }
        }
    }
}'
```
注意「_cache_key」参数的值，可以看到其值为「terms_lookup_order_1_books」，它里面包含了顾客id信息。请注意，如果给不同的查询设置了相同的「_cache_key」，那么结果就会出现不可预知的错误。这是因为ElasticSearch会基于指定的key来存储查询结果，然后在不同的查询中复用。

### 「Term Filter」的工作原理
回顾我们发送到ElasticSearch的查询命令。可以看到，它只是一个简单的过滤查询，包含一个全量查询和一个terms 过滤器。只是该查询命令中，terms 过滤器使用了一种不同的技巧：不是明确指定某些term的值，而是从其它的索引中动态加载。

我们的过滤器基于id域，这是因为只需要id域就整合了其它所有的属性。接下来就需要关注id域中的新属性: index,type,id,path。
* index: 指明了加载terms的索引源(本例是orders索引)。
* type: 告诉ElasticSearch我们的目标文档类型(本例是order类型)。
* id: 指明我们在指定索引的文档类型中的目标文档。
* path: 告诉ElasticSearch应该从哪个域中加载term(本例是orders索引的books域)。 

总结：ElasticSearch所做的工作就是从orders索引的order文档类型中，id为1的文档里加载books域中的term。这些取得的值将用于「terms filter」来过滤从books索引(命令执行的目的地是books索引)中查询到的文档，过滤条件是文档id域(本例中terms filter名称为id)的值在过滤器中存在。

为了提供「terms lookup」功能，ElasticSearch引入一种新的Cache类型，该类型的缓存基于LRU[Least Recently Used]策略。

ElasticSearch 在 elasticsearch.yml 配置文件中提供如下参数来设置该缓存：
* indices.cache.filter.terms.size: 默认值为10mb；指定了ES用于「terms lookup」缓存的内存的最大容量。根据业务场景适当调整。
* indices.cache.filter.terms.expire_after_access: 该属性指定了缓存项最后一次访问到失效的最大时间。默认该属性关闭，即永不失效。
* indices.cache.filter.terms.expire_after_write: 该属性指定了缓存项第一次写入到失效的最大时间。默认该属性关闭，即永不失效。
