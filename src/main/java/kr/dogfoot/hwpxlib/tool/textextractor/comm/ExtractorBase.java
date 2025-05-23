package kr.dogfoot.hwpxlib.tool.textextractor.comm;

import kr.dogfoot.hwpxlib.object.common.HWPXObject;
import kr.dogfoot.hwpxlib.object.common.ObjectType;
import kr.dogfoot.hwpxlib.tool.textextractor.Parameter;
import kr.dogfoot.hwpxlib.tool.textextractor.paraHead.ParaHeadMaker;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;

public abstract class ExtractorBase {
    protected final ExtractorManager extractorManager;
    protected final Parameter parameter;

    public ExtractorBase(ExtractorManager extractorManager, Parameter parameter) {
        this.extractorManager = extractorManager;
        this.parameter = parameter;
    }

    public abstract ObjectType _objectType();

    public abstract void extract(HWPXObject from) throws Exception;

    public void extractChild(HWPXObject child) throws Exception {
        ExtractorBase extractor = extractorManager.get(child._objectType());
        extractor.extract(child);
        extractorManager.release(extractor);
    }

    // New method to extract child content as a String
    protected String extractChildToString(HWPXObject child) throws Exception {
        Parameter tempParameter = new Parameter(
                parameter.hwpxFile(), // HWPXFile 인스턴스 (현재 ExtractorBase에서 직접 접근 불가)
                parameter.textExtractMethod(), // 수정: getTextExtractMethod() -> textExtractMethod()
                parameter.insertParaHead(),    // 수정: isInsertParaHead() -> insertParaHead()
                parameter.textMarks() // TextMarks는 Parameter 내에서 TextBuilder 생성 시 사용되므로, 새롭게 생성
        );

        ExtractorManager tempExtractorManager = new ExtractorManager(tempParameter);
        ExtractorBase childExtractor = tempExtractorManager.get(child._objectType());

        if (childExtractor._objectType() == ObjectType.Unknown) {
            return "";
        }

        childExtractor.extract(child);
        tempExtractorManager.release(childExtractor);

        return tempParameter.result();
    }

    protected ParaHeadMaker paraHeadMaker() {
        return parameter.paraHeadMaker();
    }

    protected TextBuilder textBuilder() {
        return parameter.textBuilder();
    }
}
