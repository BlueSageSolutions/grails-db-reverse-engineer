/* Copyright 2010-2012 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.reveng

import org.hibernate.tool.hbm2x.Cfg2HbmTool
import org.hibernate.tool.hbm2x.Cfg2JavaTool
import org.hibernate.tool.hbm2x.POJOExporter
import org.hibernate.tool.hbm2x.pojo.POJOClass

class HydraExporter extends POJOExporter {

	protected Cfg2HbmTool c2h
	protected GrailsCfg2HydraTool c2j
	protected boolean overwrite
	protected ConfigObject revengConfig

    HydraExporter(boolean overwrite, ConfigObject revengConfig) {
		this.overwrite = overwrite
		c2h = new Cfg2HbmTool()
		c2j = new GrailsCfg2HydraTool(c2h, getConfiguration(), revengConfig)
	}

	@Override
	protected void init() {
		setTemplateName getClass().getPackage().name.replace('.', '/') + '/HydraClass.ftl'
    	setFilePattern 'com/bss/hydra/graphql/queries/{class-name}.groovy'
	}

	@Override
	Cfg2HbmTool getCfg2HbmTool() { c2h }

	@Override
	Cfg2JavaTool getCfg2JavaTool() { c2j }

	@Override
	protected void exportPOJO(Map additionalContext, POJOClass element) {
		GrailsTemplateProducer producer = new GrailsTemplateProducer(
			getTemplateHelper(), getArtifactCollector(), overwrite)
		additionalContext.pojo = element
		additionalContext.clazz = element.decoratedObject
		String filename = resolveFilename(element)
		// Ignore "components" - not really sure why it works like this, but a "component" looks
		// like it has something to do with IDs, e.g., element.toString() = Component: lendingplatform.VLoanLockStatusId
		if (!element.toString().contains("Component:")) {
			producer.produce additionalContext, getTemplateName(),
				new File(getOutputDirectory(), filename),
				templateName, element.toString()
		}
	}
}
