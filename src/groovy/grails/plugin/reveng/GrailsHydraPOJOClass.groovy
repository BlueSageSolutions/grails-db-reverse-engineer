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

import org.hibernate.cfg.Configuration
import org.hibernate.mapping.*
import org.hibernate.tool.hbm2x.Cfg2HbmTool
import org.hibernate.tool.hbm2x.Cfg2JavaTool
import org.hibernate.tool.hbm2x.pojo.EntityPOJOClass
import org.hibernate.type.*

import java.util.Map
import java.util.Set

class GrailsHydraPOJOClass extends EntityPOJOClass {


    protected PersistentClass clazz
    protected Cfg2HbmTool c2h
    protected Configuration configuration
    protected ConfigObject revengConfig
    protected String newline = System.getProperty('line.separator')

    GrailsHydraPOJOClass(PersistentClass clazz, Cfg2JavaTool cfg, Cfg2HbmTool c2h,
                         Configuration configuration, ConfigObject revengConfig) {
		super(clazz, cfg)
		this.clazz = clazz
		this.c2h = c2h
		this.configuration = configuration
		this.revengConfig = revengConfig
	}

	String renderClassStart() {
        String declarationName = getDeclarationName()
		"class ${declarationName} {"
	}

}
