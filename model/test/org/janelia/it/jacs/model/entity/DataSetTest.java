
package org.janelia.it.jacs.model.entity;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.janelia.it.jacs.model.user_data.User;

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
public class DataSetTest extends TestCase {

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

        Assert.assertEquals("entity name not preserved",
                            name, dataSet.getName());
        Assert.assertEquals("entity userLogin not preserved",
        					ownerKey, dataSet.getUser());
        Assert.assertEquals("entity dataSetIdentifier not preserved",
                            dataSetIdentifier, dataSet.getDataSetIdentifier());
        Assert.assertEquals("entity sageSync not preserved",
                sageSync, dataSet.getSageSync());
        Assert.assertTrue("hasSageSync should return true",
                dataSet.hasSageSync());

        JAXBContext ctx = JAXBContext.newInstance(DataSet.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();

        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<dataSet>\n" +
                "    <dataSetIdentifier>leetlab_pan_lineage_40x</dataSetIdentifier>\n" +
                "    <name>Pan Lineage 40x</name>\n" +
                "    <sageSync>SAGE Sync</sageSync>\n" +
                "    <user>leetlab</user>\n" +
                "</dataSet>\n";

        m.marshal(dataSet, writer);
        final StringBuffer sb = writer.getBuffer();
        Assert.assertEquals("JAXB marshalled xml does not match",
                            xml, sb.toString());
    }

    private EntityAttribute getEntityAttribute(String name) {
        return new EntityAttribute(null, name, null, null, null);
    }

    private EntityData getEntityData(String attrName, String ownerKey, String value) {
        return new EntityData(null, attrName, null, null, ownerKey, value, null, null, null);
    }

}