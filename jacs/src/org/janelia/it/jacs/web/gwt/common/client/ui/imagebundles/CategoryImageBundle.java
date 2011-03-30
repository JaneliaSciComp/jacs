/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * ImageBundle for images representing categories (Search, Projects, etc.).
 *
 * @author Michael Press
 */
public interface CategoryImageBundle extends ImageBundle {
    @ImageBundle.Resource("imagebundle_images/categories/AccessionIconTiny.png")
    public AbstractImagePrototype getAccessionIconTiny();

    @ImageBundle.Resource("imagebundle_images/categories/AccessionIcon_v1.png")
    public AbstractImagePrototype getAccessionIconSmall();

    @ImageBundle.Resource("imagebundle_images/categories/AccessionIconLarge_v2.png")
    public AbstractImagePrototype getAccessionIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/BLASTIconLarge_v2.png")
    public AbstractImagePrototype getBlastIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/FragmentRecruitmentLarge_v1.png")
    public AbstractImagePrototype getFragmentRecruitmentIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/GenomesIcon_v1.png")
    public AbstractImagePrototype getGenomesIconSmall();

    @ImageBundle.Resource("imagebundle_images/categories/GenomesIconLarge_v1.png")
    public AbstractImagePrototype getGenomesIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/MyBlastJobsIcon_v1.png")
    public AbstractImagePrototype getMyBlastJobsIconSmall();

    @ImageBundle.Resource("imagebundle_images/categories/MyBlastJobsIconLarge_v1.png")
    public AbstractImagePrototype getMyBlastJobsIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/NonCodingGenesIcon_v1.png")
    public AbstractImagePrototype getNonCodingGenesIconSmall();

    @ImageBundle.Resource("imagebundle_images/categories/NonCodingGenesIconLarge_v1.png")
    public AbstractImagePrototype getNonCodingGenesIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/ProjectSamplesIconTiny.png")
    public AbstractImagePrototype getSamplesIconTiny();

    @ImageBundle.Resource("imagebundle_images/categories/ProjectSamplesIconMap_v3.png")
    public AbstractImagePrototype getSamplesIconSmall();

    @ImageBundle.Resource("imagebundle_images/categories/ProjectSamplesIconLargeMap_v2.png")
    public AbstractImagePrototype getSamplesIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/ProjectsIconTiny.png")
    public AbstractImagePrototype getProjectsIconTiny();

    @ImageBundle.Resource("imagebundle_images/categories/ProjectsIcon_v1.png")
    public AbstractImagePrototype getProjectsIconSmall();

    @ImageBundle.Resource("imagebundle_images/categories/ProjectsIconLarge_v1.png")
    public AbstractImagePrototype getProjectsIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/ProteinIconTiny.png")
    public AbstractImagePrototype getProteinsIconTiny();

    @ImageBundle.Resource("imagebundle_images/categories/ProteinIcon_v1.png")
    public AbstractImagePrototype getProteinsIconSmall();

    @ImageBundle.Resource("imagebundle_images/categories/ProteinIconLarge_v1.png")
    public AbstractImagePrototype getProteinsIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/ProteinClusterIconTiny.png")
    public AbstractImagePrototype getProteinClustersIconTiny();

    @ImageBundle.Resource("imagebundle_images/categories/ProteinClusterIcon_v1.png")
    public AbstractImagePrototype getProteinClustersIconSmall();

    @ImageBundle.Resource("imagebundle_images/categories/ProteinClusterIconLarge_v1.png")
    public AbstractImagePrototype getProteinClustersIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/PublicationsIconTiny.png")
    public AbstractImagePrototype getPublicationsIconTiny();

    @ImageBundle.Resource("imagebundle_images/categories/PublicationsIcon_v2.png")
    public AbstractImagePrototype getPublicationsIconSmall();

    @ImageBundle.Resource("imagebundle_images/categories/PublicationsIconLarge_v2.png")
    public AbstractImagePrototype getPublicationsIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/All3x3Icon_v1.png")
    public AbstractImagePrototype getSearchAllIconSmall();

    @ImageBundle.Resource("imagebundle_images/categories/CameraWebsiteIconTiny.png")
    public AbstractImagePrototype getWebsiteIconTiny();

    @ImageBundle.Resource("imagebundle_images/categories/CameraWebsiteIcon.png")
    public AbstractImagePrototype getWebsiteIconSmall();

    @ImageBundle.Resource("imagebundle_images/categories/CameraWebsiteIconLarge.png")
    public AbstractImagePrototype getWebsiteIconLarge();

    @ImageBundle.Resource("imagebundle_images/categories/MarineMicroLogo.jpg")
    public AbstractImagePrototype getMooreProjectLogo();
}
