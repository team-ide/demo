package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"os"
	"time"

	elastic "github.com/olivere/elastic/v7"
	"github.com/tealeg/xlsx"
)

func GetEsClient() *elastic.Client {
	file := "./eslog.log"
	logFile, _ := os.OpenFile(file, os.O_RDWR|os.O_CREATE|os.O_APPEND, 0766) // 应该判断error，此处简略
	client, err := elastic.NewClient(
		elastic.SetURL("http://192.168.6.84:21110/"),
		//docker
		elastic.SetSniff(false),
		elastic.SetInfoLog(log.New(logFile, "ES-INFO: ", 0)),
		elastic.SetTraceLog(log.New(logFile, "ES-TRACE: ", 0)),
		elastic.SetErrorLog(log.New(logFile, "ES-ERROR: ", 0)),
	)

	if err != nil {
		panic(err)
	}

	return client
}

func main() {
	deleteIndex("user_index_name")
	createIndex("user_index_name")
	putMapping("user_index_name", "onlineDevices", "nested")
	process(0, "")
	// outUser()
}

func outUser() {
	client := GetEsClient()
	defer client.Stop()

	// search := client.Search("user_index_name_bak")
	search := client.Search("user_index_name")
	query := elastic.NewBoolQuery()
	// query.Must(elastic.NewWildcardQuery("name", "朱亮"))
	res, err := search.Query(query).Size(500).From(0).TrackTotalHits(true).Do(context.Background())
	if err != nil {
		panic(err)
	}

	file := xlsx.NewFile()
	sheet, err := file.AddSheet("Sheet")
	if err != nil {
		panic(err)
	}
	row := sheet.AddRow()
	cell := row.AddCell()
	cell.Value = "用户ID"
	cell = row.AddCell()
	cell.Value = "用户名称"
	cell = row.AddCell()
	cell.Value = "登录账号"
	cell = row.AddCell()
	cell.Value = "手机"
	cell = row.AddCell()
	cell.Value = "邮箱"
	// println(ToJSON(res.Hits.TotalHits))
	// println(ToJSON(res.Hits.Hits))
	for _, hit := range res.Hits.Hits {
		userDoc := map[string]interface{}{}
		err = json.Unmarshal(hit.Source, &userDoc)
		if err != nil {
			panic(err)
		}
		var loginAccount interface{}
		var email interface{}
		var phone interface{}
		if userDoc["accountList"] != nil {
			var bs []byte
			bs, err = json.Marshal(userDoc["accountList"])
			if err != nil {
				panic(err)
			}
			fmt.Println("accountList:", string(bs))
			var list []map[string]interface{}
			err = json.Unmarshal(bs, &list)
			if err != nil {
				panic(err)
			}
			fmt.Println("list:", list)
			for _, one := range list {
				var t float64 = one["type"].(float64)
				if t == 7 {
					loginAccount = one["contactInfo"]
				} else if t == 3 {
					email = one["contactInfo"]
				} else if t == 1 {
					phone = one["contactInfo"]
				}
			}
		}
		row := sheet.AddRow()
		cell := row.AddCell()
		cell.Value = fmt.Sprint(userDoc["userID"])
		cell = row.AddCell()
		cell.Value = fmt.Sprint(userDoc["name"])
		cell = row.AddCell()
		cell.Value = fmt.Sprint(loginAccount)
		cell = row.AddCell()
		cell.Value = fmt.Sprint(phone)
		cell = row.AddCell()
		cell.Value = fmt.Sprint(email)

	}
	err = file.Save("导出用户.xlsx")
	if err != nil {
		panic(err)
	}
}

func deleteIndex(indexName string) {

	client := GetEsClient()
	defer client.Stop()

	res, err := client.DeleteIndex(indexName).Do(context.Background())
	if err != nil {
		panic(err)
	}
	println("deleteIndex:" + indexName)
	println("deleteIndex res:" + ToJSON(res))
}
func createIndex(indexName string) {

	client := GetEsClient()
	defer client.Stop()

	res, err := client.CreateIndex(indexName).Do(context.Background())
	if err != nil {
		panic(err)
	}
	println("createIndex:" + indexName)
	println("createIndex res:" + ToJSON(res))
}
func putMapping(indexName string, fieldName string, fieldType string) {

	client := GetEsClient()
	defer client.Stop()
	bodyJSON := map[string]interface{}{}
	// bodyJSON["mappings"] = map[string]interface{}{
	// 	"_doc": map[string]interface{}{
	// 		"properties": map[string]interface{}{
	// 			fieldName: map[string]interface{}{
	// 				"type": fieldType,
	// 			},
	// 		},
	// 	},
	// }
	bodyJSON["properties"] = map[string]interface{}{
		fieldName: map[string]interface{}{
			"type": fieldType,
		},
	}
	res, err := client.PutMapping().Index(indexName).BodyJson(bodyJSON).Do(context.Background())
	if err != nil {
		panic(err)
	}
	println("putMapping:" + indexName + "," + fieldName + "," + fieldType)
	println("putMapping res:" + ToJSON(res))
}

func process(pageIndex int, ScrollId string) {
	pageSize := 100
	client := GetEsClient()
	defer client.Stop()

	// search := client.Search("user_index_name_bak")
	search := client.Scroll("user_index_name_bak").ScrollId(ScrollId)
	query := elastic.NewBoolQuery()
	// query.Must(elastic.NewWildcardQuery("name", "朱亮"))
	res, err := search.Query(query).Size(pageSize).TrackTotalHits(true).Do(context.Background())
	if err != nil {
		panic(err)
	}
	ScrollId = res.ScrollId

	// println(ToJSON(res.Hits.TotalHits))
	// println(ToJSON(res.Hits.Hits))
	bulkService := client.Bulk()
	for _, hit := range res.Hits.Hits {
		userDoc := map[string]interface{}{}
		err = json.Unmarshal(hit.Source, &userDoc)
		if err != nil {
			panic(err)
		}
		delete(userDoc, "lastOnlineTime")
		delete(userDoc, "deviceTypes")

		onlineDevices := []map[string]interface{}{}
		num := RandInt(0, 10)
		switch num {
		case 1:
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":   1,
				"onlineStatus": 1,
				"onlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(0, 5)),
			})
		case 2:
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":   2,
				"onlineStatus": 1,
				"onlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(0, 5)),
			})
		case 3:
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":   3,
				"onlineStatus": 1,
				"onlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(0, 5)),
			})
		case 4:
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":   1,
				"onlineStatus": 1,
				"onlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(0, 5)),
			})
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":   2,
				"onlineStatus": 1,
				"onlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(0, 5)),
			})
		case 5:
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":   1,
				"onlineStatus": 1,
				"onlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(0, 5)),
			})
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":   3,
				"onlineStatus": 1,
				"onlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(0, 5)),
			})
		case 6:
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":   1,
				"onlineStatus": 1,
				"onlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(0, 5)),
			})
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":   2,
				"onlineStatus": 1,
				"onlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(0, 5)),
			})
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":   3,
				"onlineStatus": 1,
				"onlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(0, 5)),
			})
		case 7:
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":   1,
				"onlineStatus": 1,
				"onlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(0, 5)),
			})
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":      2,
				"iosOnlineStatus": 1,
				"iosOnlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(5, 30)),
			})
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":      3,
				"iosOnlineStatus": 1,
				"iosOnlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(5, 30)),
			})
		case 8:
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":      2,
				"iosOnlineStatus": 1,
				"iosOnlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(5, 30)),
			})
			onlineDevices = append(onlineDevices, map[string]interface{}{
				"deviceType":      3,
				"iosOnlineStatus": 1,
				"iosOnlineTime":   GetNowTime() - 60*60*1000*int64(RandInt(5, 30)),
			})
		default:

		}
		onlineDeviceTypes := []int{}
		lastOnlineTime := GetNowTime() - 60*60*1000*int64(RandInt(5, 30))
		for _, onlineDevice := range onlineDevices {
			if onlineDevice["onlineStatus"] == nil {
				onlineDevice["onlineStatus"] = 2
			}
			if onlineDevice["iosOnlineStatus"] == nil {
				onlineDevice["iosOnlineStatus"] = 2
			}
			if onlineDevice["onlineStatus"] == 1 {
				onlineDeviceTypes = append(onlineDeviceTypes, onlineDevice["deviceType"].(int))
				if lastOnlineTime < onlineDevice["onlineTime"].(int64) {
					lastOnlineTime = onlineDevice["onlineTime"].(int64)
				}
			} else if onlineDevice["iosOnlineStatus"] == 1 {
				onlineDeviceTypes = append(onlineDeviceTypes, onlineDevice["deviceType"].(int))
				if lastOnlineTime < onlineDevice["iosOnlineTime"].(int64) {
					lastOnlineTime = onlineDevice["iosOnlineTime"].(int64)
				}
			}
		}
		userDoc["onlineDevices"] = onlineDevices
		userDoc["onlineDeviceTypes"] = onlineDeviceTypes
		userDoc["lastOnlineTime"] = lastOnlineTime
		userDoc["onlineDeviceTypeSize"] = len(onlineDeviceTypes)

		bulkService.Add(elastic.NewBulkCreateRequest().Index("user_index_name").Id(hit.Id).Doc(userDoc))
	}
	response, err := bulkService.Do(context.Background())
	if err != nil {
		panic(err)
	}

	println("处理完成：", len(response.Items))
	println("已处理：", pageIndex*pageSize+len(response.Items))

	if len(res.Hits.Hits) == pageSize {
		process(pageIndex+1, ScrollId)
	}
}

func ToJSON(data interface{}) string {
	if data != nil {
		bs, _ := json.Marshal(data)
		if bs != nil {
			return string(bs)
		}
	}
	return ""
}

//获取当前时间戳
func GetNowTime() int64 {
	return time.Now().UnixNano() / 1e6
}

// 获取随机数
func RandInt(min int, max int) int {
	if max < min {
		panic(fmt.Sprint("RandInt error,min:", min, ",max:", max))
	}
	//设置随机数种子
	rand.Seed(time.Now().UnixNano())
	return min + rand.Intn(max-min)
}
