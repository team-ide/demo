package main

import (
	"fmt"
	"strconv"

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
	cell.Value = "组织架构排序"
	cell = row.AddCell()
	cell.Value = "员工级别"
	for g1i := 1; g1i <= 5; g1i++ {
		g1 := strconv.Itoa(g1i)
		if g1i < 10 {
			g1 = "0" + strconv.Itoa(g1i)
		}
		g1Name := fmt.Sprint("部门", g1)

		orgFullName := fmt.Sprint(g1Name)
		orgAccount := fmt.Sprint("110", g1, "00", "00")
		orgUser := fmt.Sprint(g1Name, "-", "用户")
		appendUser(sheet, 5, orgAccount, orgUser, orgFullName)

		for g2i := 1; g2i <= 5; g2i++ {
			g2 := strconv.Itoa(g2i)
			if g2i < 10 {
				g2 = "0" + strconv.Itoa(g2i)
			}
			g2Name := fmt.Sprint(g1Name, "", g2)

			orgFullName := fmt.Sprint(g1Name, "-", g2Name)
			orgAccount := fmt.Sprint("110", g1, g2, "00")
			orgUser := fmt.Sprint(g2Name, "-", "用户")
			appendUser(sheet, 5, orgAccount, orgUser, orgFullName)

			for g3i := 1; g3i <= 5; g3i++ {
				g3 := strconv.Itoa(g3i)
				if g3i < 10 {
					g3 = "0" + strconv.Itoa(g3i)
				}
				g3Name := fmt.Sprint(g2Name, "", g3)

				orgFullName := fmt.Sprint(g1Name, "-", g2Name, "-", g3Name)
				orgAccount := fmt.Sprint("110", g1, g2, g3)
				orgUser := fmt.Sprint(g3Name, "-", "用户")
				appendUser(sheet, 5, orgAccount, orgUser, orgFullName)
			}
		}
	}

	err = file.Save("组织和人员.xlsx")
	if err != nil {
		panic(err)
	}
}

func appendUser(sheet *xlsx.Sheet, size int, orgAccount string, orgUser string, orgFullName string) {
	for ui := 1; ui <= size; ui++ {
		u := strconv.Itoa(ui)
		if ui < 10 {
			u = "0" + strconv.Itoa(ui)
		}
		account := fmt.Sprint(orgAccount, u)
		user := fmt.Sprint(orgUser, "-", ui)
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
		cell.Value = user
		cell = row.AddCell()
		cell.Value = ""
		cell = row.AddCell()
		cell.Value = orgFullName
		cell = row.AddCell()
		cell.Value = "1"
		cell = row.AddCell()
		cell.Value = ""
	}
}
