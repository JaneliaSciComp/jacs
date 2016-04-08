
package org.janelia.it.jacs.model.entity;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.entity.json.JsonDataSet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests the {@link JsonDataSet} class.
 *
 * @author Eric Trautman
 */
@Category(TestCategories.FastTests.class)
public class DataSetTest {

    @Test
    public void testModel() throws Exception {

        final String name = "Pan Lineage 40x";
        final String ownerKey = "group:leetlab";
        final String dataSetIdentifier = "leetlab_pan_lineage_40x";
        final Boolean sageSync = true;
        
        DataSet dataSet = new DataSet();
        dataSet.setName(name);
        dataSet.setOwnerKey(ownerKey);
        dataSet.setIdentifier(dataSetIdentifier);
        dataSet.setSageSync(sageSync);
        JsonDataSet jsonDataSet = new JsonDataSet(dataSet);

        Assert.assertEquals("entity name not preserved", name, jsonDataSet.getName());
        Assert.assertEquals("entity userLogin not preserved", ownerKey, jsonDataSet.getUser());
        Assert.assertEquals("entity dataSetIdentifier not preserved",
                            dataSetIdentifier, jsonDataSet.getDataSetIdentifier());
        Assert.assertEquals("entity sageSync not preserved", sageSync, jsonDataSet.getSageSync());
        Assert.assertTrue("hasSageSync should return true", jsonDataSet.hasSageSync());

        JAXBContext ctx = JAXBContext.newInstance(JsonDataSet.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();

        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<dataSet>\n" +
                "    <dataSetIdentifier>" + dataSetIdentifier + "</dataSetIdentifier>\n" +
                "    <name>" + name + "</name>\n" +
                "    <sageSync>" + sageSync + "</sageSync>\n" +
                "    <user>" + ownerKey + "</user>\n" +
                "</dataSet>\n";

        m.marshal(dataSet, writer);
        final StringBuffer sb = writer.getBuffer();
        Assert.assertEquals("JAXB marshalled xml does not match",
                            xml, sb.toString());
    }

}