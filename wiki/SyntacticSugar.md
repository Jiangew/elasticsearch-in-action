# Elasticsearch in Action

## Elasticsearch 查询语法糖
### 基本搜索
```json
{
    "query": {
        "bool": {
            "must": [
                {
                    "match_all": {}
                }
            ]
        }
    },
    "from": 0,
    "size": 1
}
```

### Group By
```json
{
    "query": {
        "bool": {
            "must": [
                {
                    "match_all": {}
                }
            ]
        }
    },
    "from": 0,
    "size": 0,
    "aggregations": {
        "mid": {
            "aggregations": {
                "terminal": {
                    "terms": {
                        "field": "terminal",
                        "size": 0
                    }
                }
            },
            "terms": {
                "field": "mid",
                "size": "1"
            }
        }
    }
}
```

### Distinct Count
```json
{
    "query": {
        "bool": {
            "must": [
                {
                    "match_all": {}
                }
            ]
        }
    },
    "from": 0,
    "size": 0,
    "aggregations": {
        "COUNT(distinct (mid))": {
            "cardinality": {
                "field": "(mid)"
            }
        }
    }
}
```

### 全文搜索
```json
{
    "query" : {
        "query_string" : {"query" : "name:rcx"}
    }
}
```

### match 查询
```json
{
    "query": {
        "match": {
            "title": "crime and punishment"
        }
    }
}
```

### 通配符查询
```json
{
    "query": {
        "wildcard": {
             "title": "cr?me"
        }
    }
}
```

### 范围查询
```json
{
    "query": {
        "range": {
             "year": {
                  "gte" :1890,
                  "lte":1900
              }
        }
    }
}
```

### 正则表达式查询
```json
{
    "query": {
        "regexp": {
             "title": {
                  "value" :"cr.m[ae]",
                  "boost":10.0
              }
        }
    }
}
```

### 布尔查询
```json
{
    "query": {
        "bool": {
            "must": {
                "term": {
                    "title": "crime"
                }
            },
            "should": {
                "range": {
                    "year": {
                        "from": 1900,
                        "to": 2000
                    }
                }
            },
            "must_not": {
                "term": {
                    "otitle": "nothing"
                }
            }
        }
    }
}
```
