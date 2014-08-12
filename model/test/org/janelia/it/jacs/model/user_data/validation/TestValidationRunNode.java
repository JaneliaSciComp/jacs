package org.janelia.it.jacs.model.user_data.validation;

import org.janelia.it.jacs.model.TestCategories;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by fosterl on 8/12/14.
 */
@Category(TestCategories.FastTests.class)
public class TestValidationRunNode {
    @Test
    public void sanitizeNodeName() {
        Assert.assertTrue("VT_MCFO_Case_1".toLowerCase().equals(ValidationRunNode.sanitizeNodeName("VT MCFO Case 1")));
        Assert.assertTrue("Nodename__please_Change_this_".toLowerCase().equals(ValidationRunNode.sanitizeNodeName("Nodename, please Change this!")));
        Assert.assertTrue("This__shall_be_changed_renovated_overrated_away_go____".toLowerCase().equals(ValidationRunNode.sanitizeNodeName("This: shall be changed/renovated\\overrated;away-go$%^&")));
        Assert.assertTrue("This_is_the_killer___".toLowerCase().equals(ValidationRunNode.sanitizeNodeName("This[is]the killer^~?")));
    }
}
