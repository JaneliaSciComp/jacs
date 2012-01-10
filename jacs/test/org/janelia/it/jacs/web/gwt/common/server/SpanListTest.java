
package org.janelia.it.jacs.web.gwt.common.server;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.janelia.it.jacs.web.gwt.common.client.ui.Span;
import org.janelia.it.jacs.web.gwt.common.client.ui.SpanList;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Mar 12, 2007
 * Time: 10:58:08 AM
 *
 */
public class SpanListTest extends TestCase {
    public void testSplitsShort() {
        //ZCXVXCBNVMBVJFHDGFSDFAS
        Span spanToSplit = new Span("ZCXV<br/>XCBNVM<br/>BVJFHDGFSDFAS","mystyle");
        Span span = new Span("ZCXV<br/>XCBNVM<br/>BVJFHDGFSDFAS","mystyle");
        assertEquals("ZCXVXCBNVMBVJFHDGFSDFAS", spanToSplit.getPcData());
        assertEquals("ZCXVXCBNVMBVJFHDGFSDFAS",span.getPcData());
        SpanList list = new SpanList();
        list.addSpan(spanToSplit);
        list.addSpan(span);
        SpanList[] spanLists = list.split(6);
        assertEquals(2,spanLists.length);
        assertEquals(1,spanLists[0].size());
        assertEquals(2,spanLists[1].size());
        assertEquals("ZCXVXCB",spanLists[0].getSpan(0).getPcData());
        assertEquals("NVMBVJFHDGFSDFAS",spanLists[1].getSpan(0).getPcData());
        assertEquals("ZCXVXCBNVMBVJFHDGFSDFAS",spanLists[1].getSpan(1).getPcData());

        assertEquals("ZCXV<br/>XCB",spanLists[0].getSpan(0).getHtml());
        assertEquals("NVM<br/>BVJFHDGFSDFAS",spanLists[1].getSpan(0).getHtml());
        assertEquals("ZCXV<br/>XCBNVM<br/>BVJFHDGFSDFAS",spanLists[1].getSpan(1).getHtml());
    }

    public void testSplitsLong() {
        //ZCXVXCBNVMBVJFHDGFSDFASRWYRUTRIYOTYUEYEXHFJFKVBNCBJJDFJDGEDYETCBCBVMKGFJDFG
        Span spanToSplit = new Span("ZCXV<br/>XCBNVM<br/>BVJFHDGFSDFAS<br/>RWYRUT<br/><br/>RIYOT<br/><br/>YUEYEXHFJ<br/>FKVBNC<br/>BJJDFJDGED<br/>YETCBCBVMK<br/>GFJDFG","mystyle");
        Span span = new Span("ZCXV<br/>XCBNVM<br/>BVJFHDGFSDFAS<br/>RWYRUT<br/><br/>RIYOT<br/><br/>YUEYEXHFJ<br/>FKVBNC<br/>BJJDFJDGED<br/>YETCBCBVMK<br/>GFJDFG","mystyle");
        assertEquals("ZCXVXCBNVMBVJFHDGFSDFASRWYRUTRIYOTYUEYEXHFJFKVBNCBJJDFJDGEDYETCBCBVMKGFJDFG",spanToSplit.getPcData());
        assertEquals("ZCXVXCBNVMBVJFHDGFSDFASRWYRUTRIYOTYUEYEXHFJFKVBNCBJJDFJDGEDYETCBCBVMKGFJDFG",span.getPcData());
        SpanList list = new SpanList();
        list.addSpan(spanToSplit);
        list.addSpan(span);
        SpanList[] spanLists = list.split(36);
        assertEquals(2,spanLists.length);
        assertEquals(1,spanLists[0].size());
        assertEquals(2,spanLists[1].size());
        assertEquals("ZCXVXCBNVMBVJFHDGFSDFASRWYRUTRIYOTYUE",spanLists[0].getSpan(0).getPcData());
        assertEquals("YEXHFJFKVBNCBJJDFJDGEDYETCBCBVMKGFJDFG",spanLists[1].getSpan(0).getPcData());
        assertEquals("ZCXVXCBNVMBVJFHDGFSDFASRWYRUTRIYOTYUEYEXHFJFKVBNCBJJDFJDGEDYETCBCBVMKGFJDFG",spanLists[1].getSpan(1).getPcData());

        assertEquals("ZCXV<br/>XCBNVM<br/>BVJFHDGFSDFAS<br/>RWYRUT<br/><br/>RIYOT<br/><br/>YUE", spanLists[0].getSpan(0).getHtml());
        assertEquals("YEXHFJ<br/>FKVBNC<br/>BJJDFJDGED<br/>YETCBCBVMK<br/>GFJDFG",spanLists[1].getSpan(0).getHtml());
        assertEquals("ZCXV<br/>XCBNVM<br/>BVJFHDGFSDFAS<br/>RWYRUT<br/><br/>RIYOT<br/><br/>YUEYEXHFJ<br/>FKVBNC<br/>BJJDFJDGED<br/>YETCBCBVMK<br/>GFJDFG",spanLists[1].getSpan(1).getHtml());
    }


    public void testSplitsInsertBreaks() {
        Span firstSpan = new Span("ZC<b>XV</b>XCBNVMBVJFHDGFSDFASR<br/>WY","mystyle");
        Span secondSpan = new Span("<b>Z</b>CXVXCBNVMBVJFHDGFSDFASRWYRUTRIYOT","mystyle");
        Span thirdSpan = new Span("ZCX<br/>","mystyle");
        SpanList spanList = new SpanList();
        spanList.addSpan(firstSpan);
        spanList.addSpan(secondSpan);
        spanList.addSpan(thirdSpan);
        SpanList[] spanLists = spanList.split(4);
        assertEquals(2,spanLists.length);
        assertEquals(1,spanLists[0].size());
        assertEquals(3,spanLists[1].size());
        assertEquals("ZC<b>XV</b>X",spanLists[0].getSpan(0).getHtml());
        assertEquals("ZCXVX",spanLists[0].getSpan(0).getPcData());

        assertEquals("CBNVMBVJFHDGFSDFASR<br/>WY",spanLists[1].getSpan(0).getHtml());
        assertEquals("CBNVMBVJFHDGFSDFASRWY",spanLists[1].getSpan(0).getPcData());

        assertEquals("<b>Z</b>CXVXCBNVMBVJFHDGFSDFASRWYRUTRIYOT",spanLists[1].getSpan(1).getHtml());
        assertEquals("ZCXVXCBNVMBVJFHDGFSDFASRWYRUTRIYOT",spanLists[1].getSpan(1).getPcData());

        assertEquals("ZCX<br/>",spanLists[1].getSpan(2).getHtml());
        assertEquals("ZCX",spanLists[1].getSpan(2).getPcData());


        spanList.insertString("<br/>",6);
        assertEquals(spanList.size(),3);
        assertEquals("ZC<b>XV</b>XC<br/>BNVMBV<br/>JFHDGF<br/>SDFASR<br/><br/>WY", spanList.getSpan(0).getHtml());
        assertEquals("<b>Z</b>CXV<br/>XCBNVM<br/>BVJFHD<br/>GFSDFA<br/>SRWYRU<br/>TRIYOT<br/>", spanList.getSpan(1).getHtml());
        assertEquals("ZCX<br/>", spanList.getSpan(2).getHtml());
    }

    public void testSplitsPlusInsert() {
        String value = getSequenceMedium();
        String preClear = value.substring(0,getClearRangeBegin());
        String clear = value.substring(getClearRangeBegin(),getClearRangeEnd());
        String postClear = value.substring(getClearRangeEnd(),value.length());

        SpanList spanList = new SpanList();
        spanList.addSpan(preClear,"bseDetailSequenceText");
        spanList.addSpan(clear,"readDetailSequenceClearRangeText");
        spanList.addSpan(postClear,"bseDetailSequenceText");

        spanList.insertString("<br/>",100);

        assertEquals("TGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAATCGGCAAACTGCCGTGGCCGGCGT<br/>CCGACGGGCGACGGTGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAATCGGCAAACT<br/>GCCGTGGCC",spanList.toStringValuesOnly());
    }

    public void testSplitsPlusInsert2() {
        String value = getSequenceLarge();
        String preClear = value.substring(0,getClearRangeBegin());
        String clear = value.substring(getClearRangeBegin(),getClearRangeEnd());
        String postClear = value.substring(getClearRangeEnd(),value.length());

        SpanList spanList = new SpanList();
        spanList.addSpan(preClear,"bseDetailSequenceText");
        spanList.addSpan(clear,"readDetailSequenceClearRangeText");
        spanList.addSpan(postClear,"bseDetailSequenceText");

        spanList.insertString("<br/>",100);

        assertEquals("TGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAATCGGCAAACTGCCGTGGCCGGCGT<br/>CCGACGGGCGACGGTGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAATCGGCAAACT<br/>GCCGTGGCCTGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAATCGGCAAACTGCCGT<br/>GGCCGGCGTCCGACGGGCGACGGTGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAAT<br/>CGGCAAACTGCCGTGGCC",spanList.toStringValuesOnly());

        SpanList[] spanListSplits = spanList.split(120);
        assertTrue((spanListSplits[0].size() + spanListSplits[1].size())>spanList.size());

    }

    public void testSplitsPlustInsert3() {
       String preClear = "GCCGGTGTGGAAGGGGGGCTCGCATGACCA";
        String clear = "TCACACCAACATCGACGCGCTGCGACAACAGGAGCAGATTGCGCTCGCGAATGTCATCGACGTACAGCGCAAGCTTGCTCGTGGGTCCGCAGCGGTCGGGTTTCATACGCTACGCGATTGCAAGGCGCGACTCGACGAAATCCAGGAAGAGCTGTTGCTGATGGGCGAATTGCCGGGGCTGTTTCTCCAGCGATATTTCTACCTCGTTGATCGCGCGACGTTGTCACGGTTGCCGAGCGGCTCGGTCGTCGTGTCCTTCCCGTCAATCGCCGCCGTCGCAGGCTTTCGCGATGGCACGATGGATGATCGCTTTGTGTGGGATGTCGAACGGTCGCTGGATCTCGCGTGTGCAGGCGTAGCGGGGGCAAGGATGAAGACGATGTCGCTGCAGGGCACGCCGACGGGATCCTCCGTGACGGGTGGAAAGCGATGAGCCGACGCCCTAAGTACATTCTGGCGGTGGCGGCCGTGTTCATCGCATGCGCGTCGGAAGGTGCTCATGCCTGGGCCGCAAATCCTGCCCGCGGTGTCATGACGCGGTGGTGGCAGTCCGCGAGTGCATGGGCCAGTGGGGTGACGTCGGGTCTTGGCGTCCAGGCTGTCCACGCGTCGTCGTCGGCCGATGTTTGCCAGCCCGAGATCGGATTTTCGCCGGAGGGTTCCGGCATCGCCTTGGTACTCAAGGCGATCGCAAGCGCGCGTCAGTCAATTCGCGTGTCAGCCTACGCCTTTACGTCGCGGGAAGTCGCGCGGGCCTTGGTCGATGCGAGAAGCCGCGGAGTTGATGTCGCAGTTGCGGTCGATGCACGACAGAACCTGACCGGTCCGGGGCGCGGGAGCGAGCGCGGCCGCGC";
        String postClear = "TGAACATGCTCGTCCGCGCTGGAATCCCGGTGCGAACGATCGAGGCGTACGCGATCCATCACGACCAGACGGTGACGATCGA";

        SpanList spanList = new SpanList();
        spanList.addSpan(preClear,"bseDetailSequenceText");
        spanList.addSpan(clear,"readDetailSequenceClearRangeText");
        spanList.addSpan(postClear,"bseDetailSequenceText");

        spanList.insertString("<br/>",105);
        assertEquals("GCCGGTGTGGAAGGGGGGCTCGCATGACCATCACACCAACATCGACGCGCTGCGACAACAGGAGCAGATTGCGCTCGCGAATGTCATCGACGTACAGCGCAAGCT<br/>TGCTCGTGGGTCCGCAGCGGTCGGGTTTCATACGCTACGCGATTGCAAGGCGCGACTCGACGAAATCCAGGAAGAGCTGTTGCTGATGGGCGAATTGCCGGGGCT<br/>GTTTCTCCAGCGATATTTCTACCTCGTTGATCGCGCGACGTTGTCACGGTTGCCGAGCGGCTCGGTCGTCGTGTCCTTCCCGTCAATCGCCGCCGTCGCAGGCTT<br/>TCGCGATGGCACGATGGATGATCGCTTTGTGTGGGATGTCGAACGGTCGCTGGATCTCGCGTGTGCAGGCGTAGCGGGGGCAAGGATGAAGACGATGTCGCTGCA<br/>GGGCACGCCGACGGGATCCTCCGTGACGGGTGGAAAGCGATGAGCCGACGCCCTAAGTACATTCTGGCGGTGGCGGCCGTGTTCATCGCATGCGCGTCGGAAGGT<br/>GCTCATGCCTGGGCCGCAAATCCTGCCCGCGGTGTCATGACGCGGTGGTGGCAGTCCGCGAGTGCATGGGCCAGTGGGGTGACGTCGGGTCTTGGCGTCCAGGCT<br/>GTCCACGCGTCGTCGTCGGCCGATGTTTGCCAGCCCGAGATCGGATTTTCGCCGGAGGGTTCCGGCATCGCCTTGGTACTCAAGGCGATCGCAAGCGCGCGTCAG<br/>TCAATTCGCGTGTCAGCCTACGCCTTTACGTCGCGGGAAGTCGCGCGGGCCTTGGTCGATGCGAGAAGCCGCGGAGTTGATGTCGCAGTTGCGGTCGATGCACGA<br/>CAGAACCTGACCGGTCCGGGGCGCGGGAGCGAGCGCGGCCGCGCTGAACATGCTCGTCCGCGCTGGAATCCCGGTGCGAACGATCGAGGCGTACGCGATCCATCA<br/>CGACCAGACGGTGACGATCGA",spanList.toStringValuesOnly());
        SpanList[] spanListSplits = spanList.split(306);
        assertEquals("GCCGGTGTGGAAGGGGGGCTCGCATGACCATCACACCAACATCGACGCGCTGCGACAACAGGAGCAGATTGCGCTCGCGAATGTCATCGACGTACAGCGCAAGCT<br/>TGCTCGTGGGTCCGCAGCGGTCGGGTTTCATACGCTACGCGATTGCAAGGCGCGACTCGACGAAATCCAGGAAGAGCTGTTGCTGATGGGCGAATTGCCGGGGCT<br/>GTTTCTCCAGCGATATTTCTACCTCGTTGATCGCGCGACGTTGTCACGGTTGCCGAGCGGCTCGGTCGTCGTGTCCTTCCCGTCAATCGCCGCCGTC",spanListSplits[0].toStringValuesOnly());
    }

    private int getClearRangeBegin() {
        return 60;
    }

    private int getClearRangeEnd() {
        return 140;
    }

    private String getSequenceMedium() {
        return "TGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAATCGGCAAACTGCCGTGGCCGGCGTCCGACGGGCGACGGTGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAATCGGCAAACTGCCGTGGCC";
    }

    private String getSequenceLarge() {
        return "TGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAATCGGCAAACTGCCGTGGCCGGCGTCCGACGGGCGACGGTGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAATCGGCAAACTGCCGTGGCCTGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAATCGGCAAACTGCCGTGGCCGGCGTCCGACGGGCGACGGTGATGGTCATGGCAACGTTCCTCCCAATTCTTCGAGGTTGGAGGTCGAACCCCAACTGCATAGCGAGGCGGGAGAATCGGCAAACTGCCGTGGCC";
    }

    public static Test suite() {
        return new TestSuite(SpanListTest.class);
    }
}
