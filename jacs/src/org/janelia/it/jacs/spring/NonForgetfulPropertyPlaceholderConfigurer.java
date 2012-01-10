
package org.janelia.it.jacs.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Nov 9, 2006
 * Time: 5:44:22 PM
 */
public class NonForgetfulPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
    /**
     * The properties used for BeanFactory post-processing.
     */
    private Properties props;

    /**
     * Gets the properties used in post-processing the BeanFactory.
     *
     * @return the properties to use in post-processing.
     */
    public Properties getProps() {
        return props;
    }

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props) throws BeansException {
        this.props = props;
        super.processProperties(beanFactoryToProcess, props);
    }
}
