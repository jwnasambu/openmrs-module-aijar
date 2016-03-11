/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p/>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p/>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.aijar;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleActivator;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.aijar.activator.HtmlFormsInitializer;
import org.openmrs.module.aijar.api.deploy.bundle.CommonMetadataBundle;
import org.openmrs.module.aijar.api.deploy.bundle.EncounterTypeMetadataBundle;
import org.openmrs.module.aijar.api.deploy.bundle.UgandaAddressMetadataBundle;
import org.openmrs.module.aijar.metadata.core.PatientIdentifierTypes;
import org.openmrs.module.appframework.service.AppFrameworkService;
import org.openmrs.module.emrapi.EmrApiConstants;
import org.openmrs.module.emrapi.utils.MetadataUtil;
import org.openmrs.module.metadatadeploy.api.MetadataDeployService;
import org.openmrs.ui.framework.resource.ResourceFactory;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 *
 * TODO: Refactor the whole class to use initializers like
 */
public class AijarActivator extends org.openmrs.module.BaseModuleActivator {

    protected Log log = LogFactory.getLog(getClass());

    /**
     * @see ModuleActivator#willRefreshContext()
     */
    public void willRefreshContext() {
        log.info("Refreshing aijar Module");
    }

    /**
     * @see ModuleActivator#contextRefreshed()
     */
    public void contextRefreshed() {
        log.info("aijar Module refreshed");
    }

    /**
     * @see ModuleActivator#willStart()
     */
    public void willStart() {
        log.info("Starting aijar Module");
    }

    /**
     * @see ModuleActivator#started()
     */
    public void started() {
        AdministrationService administrationService = Context.getAdministrationService();
        AppFrameworkService appFrameworkService = Context.getService(AppFrameworkService.class);
        MetadataDeployService deployService = Context.getService(MetadataDeployService.class);
	    ConceptService conceptService = Context.getConceptService();

        try {
            // disable the reference app registration page
            appFrameworkService.disableApp("referenceapplication.registrationapp.registerPatient");

            // install HTML Forms
            setupHtmlForms();

            // install commonly used metadata
            installCommonMetadata(deployService);

            // save defined global properties
            administrationService.saveGlobalProperties(configureGlobalProperties());

        } catch (Exception e) {
            Module mod = ModuleFactory.getModuleById("aijar");
            ModuleFactory.stopModule(mod);
            throw new RuntimeException("failed to setup the module ", e);
        }

        log.info("aijar Module started");
    }

    /**
     * Configure the global properties for the expected functionality
     *
     * @return
     */
    private List<GlobalProperty> configureGlobalProperties() {
        List<GlobalProperty> properties = new ArrayList<GlobalProperty>();
        // The auto generated OpenMRS Identifier as the primary identifier that needs to be displayed
        properties.add(
                new GlobalProperty(EmrApiConstants.PRIMARY_IDENTIFIER_TYPE, PatientIdentifierTypes.NATIONAL_ID.uuid()));

        // set the HIV care number and EID number as additional identifiers that can be searched for
        properties.add(new GlobalProperty(EmrApiConstants.GP_EXTRA_PATIENT_IDENTIFIER_TYPES,
                PatientIdentifierTypes.HIV_CARE_NUMBER.uuid() + "," + PatientIdentifierTypes.EXPOSED_INFANT_NUMBER.uuid()
                        + "," + PatientIdentifierTypes.IPD_NUMBER.uuid() + "," + PatientIdentifierTypes.ANC_NUMBER.uuid()
                        + "," + PatientIdentifierTypes.HCT_NUMBER.uuid()));

        // set the name of the application
        properties.add(new GlobalProperty("application.name", "UgandaEMR - Uganda eHealth Solution"));

        // the search mode for patients to enable searching any part of names rather than the beginning
        properties.add(new GlobalProperty("patientSearch.matchMode", "ANYWHERE"));

        // enable searching on parts of the patient identifier
        // the prefix and suffix provide a % round the entered search term with a like
        properties.add(new GlobalProperty("patient.identifierPrefix", "%"));
        properties.add(new GlobalProperty("patient.identifierSuffix", "%"));

        // the RegeX and Search patterns should be empty so that the prefix and suffix matching above can work
        properties.add(new GlobalProperty("patient.identifierRegex", ""));
        properties.add(new GlobalProperty("patient.identifierSearchPattern", ""));

        // Form Entry Settings
        properties.add(new GlobalProperty("FormEntry.enableDashboardTab", "true"));     // show as a tab on the patient dashboard
        properties.add(new GlobalProperty("FormEntry.FormEntry.enableOnEncounterTab", "true"));

        //HTML Form Entry Settings
        properties.add(new GlobalProperty("htmlformentry.showDateFormat", "false"));//Disable date format display on form entry

        //Birt Settings
        properties.add(new GlobalProperty("birt.alwaysUseOpenmrsJdbcProperties", "false"));
        properties.add(new GlobalProperty("birt.birtHome", "C:/Application Data/OpenMRS/birt/birt-runtime-2_3_2/ReportEngine"));//Eg.Users/<<username>>/Data/birt/birt-runtime-2_3_2/ReportEngine"
        properties.add(new GlobalProperty("birt.datasetDir", "C:/Application Data/OpenMRS/birt/reports/datasets"));
        properties.add(new GlobalProperty("birt.loggingDir", "C:/Application Data/OpenMRS/birt/logs"));
        properties.add(new GlobalProperty("birt.defaultReportDesignFile", "default.rptdesign"));
        properties.add(new GlobalProperty("birt.loggingLevel", "OFF"));
        properties.add(new GlobalProperty("birt.mandatory", "false"));
        properties.add(new GlobalProperty("birt.outputDir", "C:/Application Data/OpenMRS/birt/reports/output"));
        properties.add(new GlobalProperty("birt.reportDir", "C:/Application Data/OpenMRS/birt/reports"));
        properties.add(new GlobalProperty("birt.reportOutputFile", "C:/Application Data/OpenMRS/birt/reports/output/ReportOutput.pdf"));
        properties.add(new GlobalProperty("birt.reportOutputFormat", "pdf"));
        properties.add(new GlobalProperty("birt.reportPreviewFile", "C:/Application Data/OpenMRS/birt/reports/output/ReportPreview.pdf"));
       // properties.add(new GlobalProperty("birt.started", "true"));


        return properties;
    }

    private void installCommonMetadata(MetadataDeployService deployService) {
        try {
            log.info("Installing metadata");
            log.info("Installing commonly used metadata");
            deployService.installBundle(Context.getRegisteredComponents(CommonMetadataBundle.class).get(0));
            log.info("Finished installing commonly used metadata");
            log.info("Installing encounter types");
            deployService.installBundle(Context.getRegisteredComponents(EncounterTypeMetadataBundle.class).get(0));
            log.info("Finished installing encounter types");
            log.info("Installing address hierarchy");
            deployService.installBundle(Context.getRegisteredComponents(UgandaAddressMetadataBundle.class).get(0));
            log.info("Finished installing addresshierarchy");

	        // retire concepts that are duplicated in the
	        // concept metadata package
            /*ConceptService conceptService = Context.getConceptService();
            List<String> conceptsToRetire = Arrays.asList("8b64f9e1-196a-4802-a287-fd160fb97002", // YES
			        "b1629d9a-91a5-4895-b6bc-647f3a944534" // NO
			        , "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" // YES
			        , "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" // NO
			        , "1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" // UNKNOWN
			        , "1499AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"  // MODERATE
			        , "1500AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"  // SEVERE
			        , "1734AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"  // YEARS
			        , "111633AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" // URINARY TRACT INFECTION
			        , "117543AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" // HERPES ZOSTER

	        );

	        for (String uuid : conceptsToRetire) {
		        Concept concept = conceptService.getConceptByUuid(uuid);

		        if (concept != null) {
			        // retire the concept
			        log.info("Retiring concept " + concept.toString());
			        conceptService.retireConcept(concept, "Duplicated in MDS import");
			        log.info("Retired concept " + concept.toString());
		        }
	        }*/

	        // install concepts
            log.info("Installing standard metadata using the packages.xml file");
            MetadataUtil.setupStandardMetadata(getClass().getClassLoader());
            log.info("Standard metadata installed");

        } catch (Exception e) {
            Module mod = ModuleFactory.getModuleById("aijar");
            ModuleFactory.stopModule(mod);
            throw new RuntimeException("failed to install the common metadata ", e);
        }
    }

    // Method responsible for HTMLForms insertation
    private void setupHtmlForms() throws Exception {
        try {
            HtmlFormsInitializer hfi = new HtmlFormsInitializer();
            hfi.started();

        } catch (Exception e) {
            log.error("Error loading the HTML forms " + e.toString());
            // this is a hack to get component test to pass until we find the proper way to mock this
            if (ResourceFactory.getInstance().getResourceProviders() == null) {
                log.error("Unable to load HTML forms--this error is expected when running component tests");
            } else {
                throw e;
            }
        }

    }

    /**
     * @see ModuleActivator#willStop()
     */
    public void willStop() {
        log.info("Stopping aijar Module");
    }

    /**
     * @see ModuleActivator#stopped()
     */
    public void stopped() {

        log.info("aijar Module stopped");
    }
}
