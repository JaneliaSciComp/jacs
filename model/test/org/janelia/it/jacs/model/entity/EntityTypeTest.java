
package org.janelia.it.jacs.model.entity;

import org.janelia.it.jacs.model.TestCategories;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Tests the {@link EntityType} class.
 *
 * @author Eric Trautman
 */
@Category(TestCategories.FastTests.class)
public class EntityTypeTest {

    @Test
    public void testJAXB() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(EntityType.class);
        Unmarshaller unm = ctx.createUnmarshaller();
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();

        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                           "<entityType>TEST-ENTITY-TYPE-NAME</entityType>\n";

        Object o = unm.unmarshal(new StringReader(xml));
        if (o instanceof EntityType) {
            EntityType entityType = (EntityType) o;
            m.marshal(entityType, writer);
            StringBuffer sb = writer.getBuffer();
            String marshalledXml = sb.toString();
            Assert.assertEquals("input and output xml do not match", xml, marshalledXml);
        } else {
            Assert.fail("returned an object of type " + o.getClass().getName() +
                        " instead of type " + EntityType.class.getName());
        }

    }

}