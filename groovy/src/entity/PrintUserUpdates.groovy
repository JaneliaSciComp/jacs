package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*

import org.janelia.it.jacs.model.user_data.User

// Globals
username = "user:system"
f = new JacsUtils(username, false)
e = f.e
c = f.c

for(User user : c.getUsers()) {
    println "update subject set fullName='"+user.fullName+"' where name='"+user.userLogin+"';"	
}

