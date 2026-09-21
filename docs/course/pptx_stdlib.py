#!/usr/bin/env python3
"""不依赖 python-pptx：用标准库打一份可打开的课堂 PPTX。"""

from __future__ import annotations

import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

OUT = Path(__file__).resolve().parent / "10-课堂分享.pptx"

W, H = 12192000, 6858000  # 13.333" x 7.5" EMU
BG = "0B0D12"
GOLD = "C9A227"
IVORY = "F5F2EA"
MUTED = "9AA0AB"
CARD = "161A22"


def emu(inches: float) -> int:
    return int(inches * 914400)


def _ct() -> str:
    slides = "".join(
        f'<Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>'
        for i in range(1, 15)
    )
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
  {slides}
</Types>
"""


def _rels() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>
"""


def _presentation() -> str:
    sld_id = "".join(
        f'<p:sldId id="{255 + i}" r:id="rId{i}"/>'
        for i in range(1, 15)
    )
    rels = "".join(
        f'<Relationship Id="rId{i}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide{i}.xml"/>'
        for i in range(1, 15)
    )
    # presentation.xml.rels also needs master
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
  xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
  saveSubsetFonts="1">
  <p:sldMasterIdLst>
    <p:sldMasterId id="2147483648" r:id="rId15"/>
  </p:sldMasterIdLst>
  <p:sldIdLst>{sld_id}</p:sldIdLst>
  <p:sldSz cx="{W}" cy="{H}" type="screen16x9"/>
  <p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>
""", f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  {rels}
  <Relationship Id="rId15" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
</Relationships>
"""


def _theme() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Pharma">
  <a:themeElements>
    <a:clrScheme name="Pharma">
      <a:dk1><a:srgbClr val="0B0D12"/></a:dk1>
      <a:lt1><a:srgbClr val="F5F2EA"/></a:lt1>
      <a:dk2><a:srgbClr val="161A22"/></a:dk2>
      <a:lt2><a:srgbClr val="E8D58A"/></a:lt2>
      <a:accent1><a:srgbClr val="C9A227"/></a:accent1>
      <a:accent2><a:srgbClr val="C41E3A"/></a:accent2>
      <a:accent3><a:srgbClr val="3D7A5A"/></a:accent3>
      <a:accent4><a:srgbClr val="9AA0AB"/></a:accent4>
      <a:accent5><a:srgbClr val="C9A227"/></a:accent5>
      <a:accent6><a:srgbClr val="C41E3A"/></a:accent6>
      <a:hlink><a:srgbClr val="C9A227"/></a:hlink>
      <a:folHlink><a:srgbClr val="E8D58A"/></a:folHlink>
    </a:clrScheme>
    <a:fontScheme name="Pharma">
      <a:majorFont>
        <a:latin typeface="PingFang SC"/>
        <a:ea typeface="PingFang SC"/>
        <a:cs typeface="PingFang SC"/>
      </a:majorFont>
      <a:minorFont>
        <a:latin typeface="PingFang SC"/>
        <a:ea typeface="PingFang SC"/>
        <a:cs typeface="PingFang SC"/>
      </a:minorFont>
    </a:fontScheme>
    <a:fmtScheme name="Pharma">
      <a:fillStyleLst>
        <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
        <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
        <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
      </a:fillStyleLst>
      <a:lnStyleLst>
        <a:ln w="12700"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln>
        <a:ln w="12700"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln>
        <a:ln w="12700"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln>
      </a:lnStyleLst>
      <a:effectStyleLst>
        <a:effectStyle><a:effectLst/></a:effectStyle>
        <a:effectStyle><a:effectLst/></a:effectStyle>
        <a:effectStyle><a:effectLst/></a:effectStyle>
      </a:effectStyleLst>
      <a:bgFillStyleLst>
        <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
        <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
        <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
      </a:bgFillStyleLst>
    </a:fmtScheme>
  </a:themeElements>
</a:theme>
"""


def _master() -> tuple[str, str]:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
  xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg><p:bgPr><a:solidFill><a:srgbClr val="0B0D12"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
    </p:spTree>
  </p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
  <p:sldLayoutIdLst>
    <p:sldLayoutId id="2147483649" r:id="rId1"/>
  </p:sldLayoutIdLst>
</p:sldMaster>
""", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>
"""


def _layout() -> tuple[str, str]:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
  xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1">
  <p:cSld name="Blank">
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sldLayout>
""", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>
"""


def _slide_rels() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>
"""


def _txbody(lines: list[tuple[str, int, str, bool]], align="l") -> str:
    algn = {"l": "l", "c": "ctr", "r": "r"}[align]
    parts = []
    for text, size, color, bold in lines:
        b = ' b="1"' if bold else ""
        parts.append(
            f'<a:p><a:pPr algn="{algn}"><a:spcAft><a:spcPts val="1200"/></a:spcAft></a:pPr>'
            f'<a:r><a:rPr lang="zh-CN" sz="{size * 100}"{b} dirty="0">'
            f'<a:solidFill><a:srgbClr val="{color}"/></a:solidFill>'
            f'<a:latin typeface="PingFang SC"/><a:ea typeface="PingFang SC"/></a:rPr>'
            f'<a:t>{escape(text)}</a:t></a:r></a:p>'
        )
    return "<a:txBody><a:bodyPr wrap=\"square\"/><a:lstStyle/>" + "".join(parts) + "</a:txBody>"


def _box(shape_id: int, name: str, x, y, w, h, lines, align="l") -> str:
    return f"""
      <p:sp>
        <p:nvSpPr>
          <p:cNvPr id="{shape_id}" name="{name}"/>
          <p:cNvSpPr txBox="1"/>
          <p:nvPr/>
        </p:nvSpPr>
        <p:spPr>
          <a:xfrm>
            <a:off x="{x}" y="{y}"/>
            <a:ext cx="{w}" cy="{h}"/>
          </a:xfrm>
          <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
          <a:noFill/>
          <a:ln><a:noFill/></a:ln>
        </p:spPr>
        {_txbody(lines, align)}
      </p:sp>
"""


def _slide(shapes: str) -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
  xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg><p:bgPr><a:solidFill><a:srgbClr val="{BG}"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
      {shapes}
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>
"""


def _footer(page: int) -> str:
    return _box(90, "foot", emu(0.7), emu(7.05), emu(8.5), emu(0.3), [("药企进销存  ·  课堂分享", 11, MUTED, False)]) + _box(
        91, "num", emu(10.4), emu(7.05), emu(2.2), emu(0.3), [(f"{page:02d}  /  14", 11, GOLD, True)], "r"
    )


def _kicker_title(kicker: str, title: str, sub: str | None = None) -> str:
    parts = _box(2, "kicker", emu(0.7), emu(0.28), emu(12), emu(0.32), [(kicker, 12, GOLD, True)])
    parts += _box(3, "title", emu(0.7), emu(0.55), emu(12), emu(0.55), [(title, 26, IVORY, True)])
    if sub:
        parts += _box(4, "sub", emu(0.7), emu(1.08), emu(12), emu(0.35), [(sub, 13, MUTED, False)])
    return parts


def slides() -> list[str]:
    out: list[str] = []

    out.append(_slide(
        _box(2, "k", emu(1.1), emu(1.9), emu(11), emu(0.4), [("CLASS  ·  ARCHITECTURE  ·  DATABASE  ·  MODULES  ·  OPS", 13, GOLD, True)])
        + _box(3, "t", emu(1.1), emu(2.4), emu(11), emu(1.6), [("药企进销存", 40, IVORY, True), ("课堂分享", 40, IVORY, True)])
        + _box(4, "s", emu(1.1), emu(4.6), emu(11), emu(1.1), [
            ("架构 → 库 / ER → 模块 → 部署运维", 16, MUTED, False),
            ("B/S  ·  批号库存  ·  追溯  ·  日清月结", 16, MUTED, False),
        ])
    ))

    out.append(_slide(
        _kicker_title("00  /  目录", "今天按老师提纲讲四段", "不报功能清单，把结构和账讲清楚")
        + _box(10, "b", emu(0.7), emu(1.7), emu(12), emu(5.0), [
            ("01  架构     谁访问谁，请求怎么落到库", 20, IVORY, False),
            ("02  库 / ER  批号粒度，表怎么切", 20, IVORY, False),
            ("03  模块     进、存、出、码、结账", 20, IVORY, False),
            ("04  部署运维 怎么上线、备份、持续集成", 20, IVORY, False),
        ])
        + _footer(2)
    ))

    out.append(_slide(
        _kicker_title("01  /  架构", "先定系统是干什么的", "药品流通进销存，不是门店收银台")
        + _box(10, "l", emu(0.7), emu(1.7), emu(6.0), emu(5.0), [
            ("要回答的三句话", 18, GOLD, True),
            ("货在哪：某仓、某批、合格还是待验。", 16, IVORY, False),
            ("票开了没：出库单带发票号。", 16, IVORY, False),
            ("钱回来没：未回 / 部分 / 已回。", 16, IVORY, False),
            ("码能否按发票或 SPDID 查到。", 16, IVORY, False),
        ])
        + _box(11, "r", emu(7.0), emu(1.7), emu(5.6), emu(5.0), [
            ("本期主链路", 18, GOLD, True),
            ("品种 / 供应商 / 客户", 16, IVORY, False),
            ("采购入库，确认后入账", 16, IVORY, False),
            ("批号库存 → 追溯码", 16, IVORY, False),
            ("销售出库，不足拒绝", 16, IVORY, False),
            ("医院收货 → 日清 / 月结", 16, IVORY, False),
        ])
        + _footer(3)
    ))

    out.append(_slide(
        _kicker_title("01  /  架构", "逻辑分层", "客户端不碰库存 SQL")
        + _box(10, "b", emu(0.7), emu(1.65), emu(12), emu(5.2), [
            ("客户端    Vue 3 Web、uni-app X、Flutter、Electron", 18, IVORY, False),
            ("接入      Nginx：静态页走 / ，接口走 /warehouse", 18, IVORY, False),
            ("应用      Controller → Service 事务 → Mapper", 18, IVORY, False),
            ("横切      SA-Token 权限、操作日志 AOP", 18, IVORY, False),
            ("数据      MySQL pharma_ims；sys_* 与药企表同库分开", 18, IVORY, False),
        ])
        + _footer(4)
    ))

    out.append(_slide(
        _kicker_title("01  /  架构", "部署就三块", "浏览器只认识 80 / 443")
        + _box(10, "a", emu(0.7), emu(1.7), emu(3.9), emu(5.0), [
            ("Nginx", 18, GOLD, True),
            ("/  → 前端 dist", 15, IVORY, False),
            ("/warehouse → :8899", 15, IVORY, False),
            ("生产再加 HTTPS", 15, MUTED, False),
        ])
        + _box(11, "b", emu(4.8), emu(1.7), emu(3.9), emu(5.0), [
            ("Spring Boot", 18, GOLD, True),
            ("JAR 听 8899", 15, IVORY, False),
            ("上下文 /warehouse", 15, IVORY, False),
            ("确认出入库开事务", 15, IVORY, False),
        ])
        + _box(12, "c", emu(8.9), emu(1.7), emu(3.7), emu(5.0), [
            ("MySQL", 18, GOLD, True),
            ("库名 pharma_ims", 15, IVORY, False),
            ("档案 / 单据 / 库存 / 码", 15, IVORY, False),
            ("备份 backup.sh", 15, IVORY, False),
        ])
        + _footer(5)
    ))

    out.append(_slide(
        _kicker_title("02  /  库", "账不是一个商品一个数", "药品 × 仓库 × 批号 × 质量状态")
        + _box(10, "q", emu(0.7), emu(1.7), emu(12), emu(1.4), [
            ("uk_batch_stocks (drug_id, warehouse_id, batch_no, quality_status)", 18, GOLD, True),
        ], "c")
        + _box(11, "t", emu(0.7), emu(3.3), emu(12), emu(3.4), [
            ("进货确认：按入库数量加库存。草稿不动账。", 18, IVORY, False),
            ("出库确认：按批号减库存。不够就拒绝。", 18, IVORY, False),
            ("红冲：另开新单再加回，不改原蓝字。", 18, IVORY, False),
            ("升益：按实盘减账面的差额调整。", 18, IVORY, False),
        ])
        + _footer(6)
    ))

    out.append(_slide(
        _kicker_title("02  /  ER", "表按六层切", "完整图画在网页 /design")
        + _box(10, "b", emu(0.7), emu(1.65), emu(12), emu(5.2), [
            ("档案    药品、客户、供应商、仓库、员工", 18, IVORY, False),
            ("进货    进货单 + 明细，含批号效期", 18, IVORY, False),
            ("出库    出库单 + 明细；发票号、红冲、回款、收货", 18, IVORY, False),
            ("库存    批号库存、升益单", 18, IVORY, False),
            ("追溯    追溯码、包装关联，靠 SPDID 对齐", 18, IVORY, False),
            ("结账    日清、月结快照，不挂单据外键", 18, IVORY, False),
        ])
        + _footer(7)
    ))

    out.append(_slide(
        _kicker_title("02  /  ER", "几条关系就够答辩", "长文：docs/pharma-er-uml.md")
        + _box(10, "b", emu(0.7), emu(1.65), emu(12), emu(5.2), [
            ("供应商 1—N 进货单     一单一个上游", 18, IVORY, False),
            ("客户 1—N 出库单       一单一个医院或药店", 18, IVORY, False),
            ("进货/出库 1—N 明细    至少一行才能确认", 18, IVORY, False),
            ("明细 1—N 追溯码       逻辑外键 SPDID", 18, IVORY, False),
            ("红冲 N—1 原出库       original_invoice_no，不改原单", 18, IVORY, False),
            ("旧仓管 bus_*          按 SKU 扣库存，禁止混进药企图", 18, IVORY, False),
        ])
        + _footer(8)
    ))

    out.append(_slide(
        _kicker_title("03  /  模块", "sys 管人，bus 管货", "每条线都是 Controller → Service → Mapper")
        + _box(10, "b", emu(0.7), emu(1.65), emu(12), emu(5.2), [
            ("系统    用户、角色、部门、菜单、日志", 18, IVORY, False),
            ("档案    品种、客户、供应商、仓库", 18, IVORY, False),
            ("进货    草稿、确认入库、发票查询", 18, IVORY, False),
            ("出库    开票出库、红冲、回款、收货", 18, IVORY, False),
            ("仓储    批号库存、盘点、调拨、升益", 18, IVORY, False),
            ("追溯    查码、扫码纠错、大码解析", 18, IVORY, False),
            ("结账 / 报表    日清、月结、进销分析、厂家流向", 18, IVORY, False),
        ])
        + _footer(9)
    ))

    out.append(_slide(
        _kicker_title("03  /  模块", "主链路压成一句", "草稿不入账，确认才动库存")
        + _box(10, "b", emu(0.7), emu(1.7), emu(12), emu(5.1), [
            ("进 → 入 → 存 → 出 → 扣 → 收 → 清", 22, GOLD, True),
            ("采购开草稿 → 仓管确认 → 批号库存 → 销售开票", 16, IVORY, False),
            ("确认出库 → 医院签收 → 日清月结", 16, IVORY, False),
            ("库存不足：确认失败，草稿可删。", 16, MUTED, False),
            ("收货重复提交：拒绝。红冲是新单。", 16, MUTED, False),
        ])
        + _footer(10)
    ))

    out.append(_slide(
        _kicker_title("04  /  部署", "上线按这个顺序", "先备份，再换包，最后巡检")
        + _box(10, "b", emu(0.7), emu(1.65), emu(12), emu(5.2), [
            ("1  备份    deploy/backup.sh 压一份 sql.gz", 18, IVORY, False),
            ("2  后端    mvn 打 JAR，重启 8899", 18, IVORY, False),
            ("3  前端    npm run build，覆盖 Nginx dist", 18, IVORY, False),
            ("4  冒烟    登录 → 批号库存 → 一张草稿出入库", 18, IVORY, False),
            ("5  回滚    换回上一版 JAR / dist，或 restore.sh", 18, IVORY, False),
        ])
        + _footer(11)
    ))

    out.append(_slide(
        _kicker_title("04  /  运维", "日常三件事", "脚本在 deploy/，口令只走环境变量")
        + _box(10, "b", emu(0.7), emu(1.65), emu(12), emu(5.2), [
            ("备份    每天 mysqldump，保留 14 份，不放 Web 根目录", 18, IVORY, False),
            ("巡检    首页 200、公钥接口、Java/Nginx、磁盘、备份是否过期", 18, IVORY, False),
            ("生产    备案域名 + HTTPS + 岗位账号", 18, IVORY, False),
            ("不能当交付    公网 IP + admin/123456", 18, GOLD, True),
        ])
        + _footer(12)
    ))

    out.append(_slide(
        _kicker_title("04  /  CI", "持续集成：推代码就自动验", "不是“网站已经打开”")
        + _box(10, "b", emu(0.7), emu(1.65), emu(12), emu(5.2), [
            ("文件    .github/workflows/ci.yml", 18, IVORY, False),
            ("触发    push、Pull Request", 18, IVORY, False),
            ("后端    mvn -DskipTests=false test", 18, IVORY, False),
            ("前端    npm ci && npm run build", 18, IVORY, False),
            ("CI 发现有人把主分支弄坏；发布仍是手工。", 18, GOLD, True),
        ])
        + _footer(13)
    ))

    out.append(_slide(
        _box(2, "k", emu(1.1), emu(2.2), emu(11), emu(0.4), [("要看图，打开网页", 14, GOLD, True)])
        + _box(3, "t", emu(1.1), emu(2.7), emu(11), emu(1.5), [
            ("localhost:8888/design", 32, IVORY, True),
            ("未登录也能看齐 ER / 用例 / 时序", 18, MUTED, False),
        ])
        + _box(4, "s", emu(1.1), emu(4.7), emu(11), emu(1.3), [
            ("短文：docs/course/技术选型.md、架构设计.md", 16, MUTED, False),
            ("运维：deploy/ 与 docs/delivery/05-运维交付包.md", 16, MUTED, False),
        ])
    ))

    return out


def build() -> Path:
    pres_xml, pres_rels = _presentation()
    master_xml, master_rels = _master()
    layout_xml, layout_rels = _layout()
    files = {
        "[Content_Types].xml": _ct(),
        "_rels/.rels": _rels(),
        "ppt/presentation.xml": pres_xml,
        "ppt/_rels/presentation.xml.rels": pres_rels,
        "ppt/theme/theme1.xml": _theme(),
        "ppt/slideMasters/slideMaster1.xml": master_xml,
        "ppt/slideMasters/_rels/slideMaster1.xml.rels": master_rels,
        "ppt/slideLayouts/slideLayout1.xml": layout_xml,
        "ppt/slideLayouts/_rels/slideLayout1.xml.rels": layout_rels,
    }
    for i, xml in enumerate(slides(), 1):
        files[f"ppt/slides/slide{i}.xml"] = xml
        files[f"ppt/slides/_rels/slide{i}.xml.rels"] = _slide_rels()

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(OUT, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for name, data in files.items():
            zf.writestr(name, data.encode("utf-8"))
    return OUT


if __name__ == "__main__":
    path = build()
    print("saved", path, "bytes", path.stat().st_size)
