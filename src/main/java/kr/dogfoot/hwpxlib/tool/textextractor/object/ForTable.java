package kr.dogfoot.hwpxlib.tool.textextractor.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import kr.dogfoot.hwpxlib.object.common.HWPXObject;
import kr.dogfoot.hwpxlib.object.common.ObjectType;
import kr.dogfoot.hwpxlib.object.content.section_xml.enumtype.CaptionSide;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.shapeobject.Caption;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Table;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tr;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tc;
import kr.dogfoot.hwpxlib.tool.textextractor.Parameter;
import kr.dogfoot.hwpxlib.tool.textextractor.comm.ExtractorBase;
import kr.dogfoot.hwpxlib.tool.textextractor.comm.ExtractorManager;

public class ForTable extends ExtractorBase {
    public ForTable(ExtractorManager extractorManager, Parameter parameter) {
        super(extractorManager, parameter);
    }

    @Override
    public ObjectType _objectType() {
        return ObjectType.hp_tbl;
    }

    @Override
    public void extract(HWPXObject from) throws Exception {
        Table table = (Table) from;
        textBuilder().objectStart(table._objectType());
        Caption caption = table.caption();
        if (caption != null && (caption.side() == CaptionSide.TOP || caption.side() == CaptionSide.BOTTOM) && caption.subList() != null && caption.subList().paras() != null) {
            StringBuilder captionText = new StringBuilder();
            StringBuilder tempText = new StringBuilder();

            for (Para para : caption.subList().paras()) {
                tempText.append(extractChildToString(para)).append("<br>");
            }

            // String으로 변환 후 replaceAll 수행
            String cleanedText = tempText.toString().replaceAll("(<br>)+$", "");

            // 최종 결과를 captionText에 저장
            captionText.append(cleanedText);
            if (caption.side() == CaptionSide.TOP) {
                textBuilder().text("[표 제목] " + captionText.toString().trim());
            } else if (caption.side() == CaptionSide.BOTTOM) {
                textBuilder().text("[표 설명] " + captionText.toString().trim());
            }
            
            textBuilder().text("\n");
        }

        int rowCount = table.countOfTr();
        int maxCol = estimateMaxCol(table);

        String[][] buffer = new String[rowCount][maxCol];

        for (Tr tr : table.trs()) {
            if (tr.tcs() != null) {
                // 수정: `tr.tcs().items()` -> `tr.tcs()` (Iterable<Tc>에 items() 메서드 없음)
                for (Tc tc : tr.tcs()) {
                    // 수정: `getCellAddr()` -> `cellAddr()`
                    int col = tc.cellAddr().colAddr();
                    int startRow = tc.cellAddr().rowAddr();
                    int startCol = col;

                    // 수정: `getCellSpan()` -> `cellSpan()`
                    int rowSpan = tc.cellSpan() != null ? tc.cellSpan().rowSpan() : 1;
                    int colSpan = tc.cellSpan() != null ? tc.cellSpan().colSpan() : 1;

                    StringBuilder sb = new StringBuilder();
                    // 수정: `getSubList()` -> `subList()`, `getParas()` -> `paras()`
                    // `paras()`는 ListTemplate<Para>를 반환하므로 `.items()`는 유지
                    if (tc.subList() != null && tc.subList().paras() != null) {
                        Iterable<Para> paras = tc.subList().paras();
                        Iterator<Para> iter = paras.iterator();

                        while (iter.hasNext()) {
                            Para para = iter.next();
                            sb.append(extractChildToString(para));
                            if (iter.hasNext()) {
                                sb.append("<br>");  // 마지막이 아닐 때만 개행
                            }
                        }
                    }
                    String content = sb.toString().trim();

                    for (int dy = 0; dy < rowSpan; dy++) {
                        for (int dx = 0; dx < colSpan; dx++) {
                            int r = startRow + dy;
                            int c = startCol + dx;
                            if (r < rowCount && c < maxCol) {
                                buffer[r][c] = content;
                            }
                        }
                    }
                }
            }
        }

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < maxCol; c++) {
                String cellText = buffer[r][c] != null ? buffer[r][c] : "";
                if (c == 0) {
                    textBuilder().tableCellSeparator();
                }
                textBuilder().text(cellText); // 수정: `add` 대신 `write` (가정)

                if (c < maxCol) {
                    textBuilder().tableCellSeparator();
                }
            }
            if (r < rowCount - 1) {
                textBuilder().tableRowSeparator();
            }
        }

        textBuilder().objectEnd(table._objectType());
    }

    private int estimateMaxCol(Table table) {
        int maxCol = 0;
        for (Tr tr : table.trs()) {
            for (Tc tc : tr.tcs()) {
                int col = tc.cellAddr().colAddr();
                int colSpan = tc.cellSpan() != null ? tc.cellSpan().colSpan() : 1;
                maxCol = Math.max(maxCol, col + colSpan);
            }
        }
        return maxCol;
    }
}