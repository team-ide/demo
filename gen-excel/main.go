package main

import (
	"fmt"

	"github.com/tealeg/xlsx"
)

func main() {
	file := xlsx.NewFile()
	sheet, err := file.AddSheet("Sheet")
	if err != nil {
		panic(err)
	}
	row := sheet.AddRow()
	cell := row.AddCell()
	cell.Value = "自定义登录账号"
	cell = row.AddCell()
	cell.Value = "邮箱"
	cell = row.AddCell()
	cell.Value = "国家码"
	cell = row.AddCell()
	cell.Value = "手机"
	cell = row.AddCell()
	cell.Value = "用户名称"
	cell = row.AddCell()
	cell.Value = "用户排序"
	cell = row.AddCell()
	cell.Value = "组织架构名称"
	cell = row.AddCell()
	cell.Value = "员工级别"
	startAccount := 11015001010
	startUser := 1010
	for i := 0; i < 10000; i++ {
		account := startAccount + i
		user := startUser + i
		row := sheet.AddRow()
		cell := row.AddCell()
		cell.Value = fmt.Sprint(account)
		cell = row.AddCell()
		cell.Value = fmt.Sprint(account, "@linkdood.com")
		cell = row.AddCell()
		cell.Value = "0086"
		cell = row.AddCell()
		cell.Value = fmt.Sprint(account)
		cell = row.AddCell()
		cell.Value = fmt.Sprint("user", user)
		cell = row.AddCell()
		cell.Value = ""
		cell = row.AddCell()
		cell.Value = "测试"
		cell = row.AddCell()
		cell.Value = ""
	}

	err = file.Save("人员.xlsx")
	if err != nil {
		panic(err)
	}
}
