
package org.janelia.it.jacs.model.entity;

import org.janelia.it.jacs.model.TestCategories;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests the {@link DataSet} class.
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
        final String sageSync = "SAGE Sync";

        Set<EntityData> entityDataSet = new HashSet<EntityData>();
        entityDataSet.add(
                getEntityData(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER,
                              ownerKey,
                              dataSetIdentifier));
        entityDataSet.add(
                getEntityData(EntityConstants.ATTRIBUTE_SAGE_SYNC,
                              ownerKey,
                              sageSync));

        Entity entity = new Entity(null, name, ownerKey, null, null, null,
                                   entityDataSet);

        DataSet dataSet = new DataSet(entity);

        Assert.assertEquals("entity name not preserved", name, dataSet.getName());
        Assert.assertEquals("entity userLogin not preserved", ownerKey, dataSet.getUser());
        Assert.assertEquals("entity dataSetIdentifier not preserved",
                            dataSetIdentifier, dataSet.getDataSetIdentifier());
        Assert.assertEquals("entity sageSync not preserved", sageSync, dataSet.getSageSync());
        Assert.assertTrue("hasSageSync should return true", dataSet.hasSageSync());

        JAXBContext ctx = JAXBContext.newInstance(DataSet.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();

        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "TESTFAILURE<dataSet>\n" +
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

    private EntityData getEntityData(String attrName, String ownerKey, String value) {
        return new EntityData(null, attrName, null, null, ownerKey, value, null, null, null);
    }

}