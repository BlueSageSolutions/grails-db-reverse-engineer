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
import org.hibernate.mapping.Column
import org.hibernate.mapping.ForeignKey
import org.hibernate.mapping.ManyToOne
import org.hibernate.mapping.PersistentClass
import org.hibernate.mapping.Property
import org.hibernate.mapping.Table
import org.hibernate.mapping.UniqueKey
import org.hibernate.tool.hbm2x.Cfg2HbmTool
import org.hibernate.tool.hbm2x.Cfg2JavaTool
import org.hibernate.tool.hbm2x.pojo.EntityPOJOClass
import org.hibernate.type.CalendarDateType
import org.hibernate.type.CalendarType
import org.hibernate.type.DateType
import org.hibernate.type.IntegerType
import org.hibernate.type.LongType
import org.hibernate.type.TimeType
import org.hibernate.type.TimestampType
import org.hibernate.type.Type

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class GrailsEntityPOJOClass extends EntityPOJOClass {

	protected static final Map<String, String> typeNameReplacements = [
		'boolean': 'Boolean',
		'byte':    'Byte',
		'char':    'Character',
		'double':  'Double',
		'int':     'Integer',
		'float':   'Float',
		'long':    'Long',
		'short':   'Short']

    //paulb: these will be defined in BaseDomain
    private static final ignoredProperties = [
            'createdDateTime',
            'createdByUserId',
            'modifiedDateTime',
            'modifiedByUserId']

    //paulb: these will be defined in BaseRefDomain
    private static final ignoredPropertiesForRefClass = [
            'effectiveDateTime',
            'expirationDateTime'
    ]

    //paulb: these will be defined in BaseSysDomain
    private static final ignoredPropertiesForSysClass = [
            'effectiveDateTime',
            'expirationDateTime'
    ]

    //paulb: properties that contain the word 'date' but are not dates
    private static final ignoredDateProperties = [
			'autoUpdatedYn',
			'dateTypeId',
			'dateTypeDesc',
			'dueDateCalendarTypeId',
			'dueDateDays',
			'dueDateHours',
			'ntbInvalidatedYn',
			'titleUpdateFeesCount',
			'titleUpdateFeesCostAmt',
			'actionDateTimeMillis',
			'expirationDateOverrideYn',
			'requestDateTimeMillis',
			'responseDateTimeMillis',
			'batchUpdateByUserId',
			'batchUpdateDesc',
			'manualUpdateYn',
			'manualUpdateLoanLis',
			'manualUpdateContent',
			'updateFileName',
			'updateFileContent',
			'titleUpdatesCount',
			'titleUpdatesAmt',
			'titleUpdatesTotalAmt',
			'pendingBaselineUpdateYn'
	]

	//paulb: properties that contain the word 'time' but are not dates
 	private static final ignoredTimeProperties = [
			'executionTime',
			'defaultRatesEffectiveTime',
			'defaultRatesExpirationTime'
	]


    //paulb: domain names that should NOT be subclasses from BaseSysDomain
    private static final ignoredSysClasses = [
            'SysAccessItem',
            'SysAccessLoanPermission',
            'SysAccessRolePermission',
            'SysUser',
			'SysUserCommLog',
            'SysUserPasswordHistory'
    ]

    //paulb: domain names that should NOT be subclasses from BaseRefDomain
    private static final ignoredRefClasses = [
			"ReferalPartner",
			"ReferalPartnerAccessHistory",
			"ReferalPartnerActivity",
			"ReferalPartnerAlert",
			"ReferalPartnerAppointment",
			"ReferalPartnerAssignment",
			"ReferalPartnerAssociate",
			"ReferalPartnerAssocCommLog",
			"ReferalPartnerAssocPhone",
			'ReferalPartnerCommLog',
			"ReferalPartnerLicense",
			"ReferalPartnerManTsk",
			"ReferalPartnerManTskAssign",
			"ReferalPartnerManTskNote",
			"ReferalPartnerManTskStatus",
			"ReferalPartnerNote",
			"ReferalPartnerStatusHistory",
			"ReferalPartnerTask",
			"ReferalPartnerTaskAssignment",
			"ReferalPartnerTaskNote",
			"ReferalPartnerTaskStatus",
			"ReferalPartnerTelephone"
	]

	//paulb: domain classes that need a Hydra class reference generated
 	private static final generateHydraRefClasses = [
			'Contact',
			'ContactNote',
			'HomeLoan',
			'HomeLoanBorrower',
			'RefInterfaceEnumTranslation',
			'SysGlobalExternalSystem'
 	]

    protected PersistentClass clazz
    protected Cfg2HbmTool c2h
    protected Configuration configuration
    protected ConfigObject revengConfig
    protected String newline = System.getProperty('line.separator')
    protected newProperties = []

	GrailsEntityPOJOClass(PersistentClass clazz, Cfg2JavaTool cfg, Cfg2HbmTool c2h,
			Configuration configuration, ConfigObject revengConfig) {
		super(clazz, cfg)
		this.clazz = clazz
		this.c2h = c2h
		this.configuration = configuration
		this.revengConfig = revengConfig
	}

	@Override
	String getPackageDeclaration() { super.getPackageDeclaration() - ';' }

	@Override
	Iterator getAllPropertiesIterator() {
		def props = []
		def idProperty = getIdentifierProperty()
		def versionProperty = getVersionProperty()

		super.getAllPropertiesIterator().each { Property property ->
			if (property == versionProperty) {
				return
			}

			if (property == idProperty) {
				if (c2j.isComponent(property)) {
					property.value.propertyIterator.each { idProp ->
						props << idProp
					}
					return
				}
				if (property.name == 'id' || property.type instanceof LongType || property.type instanceof IntegerType) {
					return
				}
			}

			if (property.value instanceof ManyToOne || property.value instanceof org.hibernate.mapping.Set) {
				return
			}

			props << property
		}

		props.iterator()
	}

	String renderId() {
		def idProperty = getIdentifierProperty()
		if (c2j.isComponent(idProperty)) {
			def idDef = new StringBuilder('\t\tid composite: [')
			String delimiter = ''
			idProperty.value.propertyIterator.each {
				idDef.append delimiter
				idDef.append "\"${findRealIdName(it)}\""
				delimiter = ', '
			}
			idDef.append ']'
			idDef.append newline
			return idDef.toString()
		}

		if (idProperty.name != 'id' || idProperty.value.identifierGeneratorStrategy == 'assigned') {
			def idDef = new StringBuilder('\t\tid ')
			String delimiter = ''

			if (idProperty.name != 'id') {
				idDef.append delimiter
				if (idProperty.type instanceof LongType || idProperty.type instanceof IntegerType) {
					String colName = idProperty.columnIterator.next().name
					idDef.append "column: \"$colName\""
				}
				else {
					idDef.append "name: \"$idProperty.name\""
				}
				delimiter = ', '
			}

			if (idProperty.value.identifierGeneratorStrategy == 'assigned') {
				idDef.append delimiter
				idDef.append "generator: \"assigned\""
				delimiter = ', '
			}

			idDef.append newline
			return idDef.toString()
		}

		''
	}

	String renderVersion() {
		if (hasVersionProperty()) {
			if (getVersionProperty().name != 'version') {
				return '\t\tversion "' + getVersionProperty().value.columnIterator.next().name +
						'"' + newline
			}
			return ''
		}

		'\t\tversion false' + newline
	}

	String renderTable() {
//		if (clazz.table.schema || clazz.table.catalog) {
//			def tableDef = new StringBuilder('\t\ttable ')
//			String delimiter = ''
//
//			if (clazz.table.schema) {
//				tableDef.append delimiter
//				tableDef.append "schema: \"$clazz.table.quotedSchema\""
//				delimiter = ', '
//			}
//
//			if (clazz.table.catalog) {
//				tableDef.append delimiter
//				tableDef.append "catalog: \"$clazz.table.catalog\""
//				delimiter = ', '
//			}
//
//			tableDef.append newline
//			return tableDef.toString()
//		}

		''
	}

	@Override
	String generateImports() {
		def fixed = new StringBuilder()
		String imports = super.generateImports().trim()
		String delimiter = ''
		imports.eachLine { String line ->
			line -= ';'
			if (isValidImport(line - 'import ')) {
				fixed.append delimiter
				fixed.append line
				delimiter = newline
			}
		}

		if (needsEqualsHashCode()) {
			fixed.append delimiter
			fixed.append 'import org.apache.commons.lang.builder.EqualsBuilder'
			delimiter = newline
			fixed.append delimiter
			fixed.append 'import org.apache.commons.lang.builder.HashCodeBuilder'
			fixed.append delimiter
		}

        // paulb: add import for Joda and Hibernate validator constraints
        fixed.append delimiter
       	fixed.append 'import org.joda.time.*\nimport org.hibernate.validator.constraints.*\nimport javax.validation.constraints.*'
       	fixed.append delimiter

		imports = fixed.toString()
		if (imports) {
			return imports + newline + newline
		}

		''
	}

	protected boolean isValidImport(String candidate) {
		if ('java.math.BigDecimal'.equals(candidate) ||
		    'java.math.BigInteger'.equals(candidate)) {
			return false
		}

		if (isInPackage(candidate, 'java.io') ||
		    isInPackage(candidate, 'java.net') ||
		    isInPackage(candidate, 'java.util')) {
			return false
		}

		true
	}

	protected boolean isInPackage(String candidate, String pkg) {
		if (!candidate.contains(pkg)) {
			return false
		}

		int index = candidate.lastIndexOf('.')
		if (index == -1) {
			return false
		}

		String candidatePackage = candidate[0..index - 1]
		candidatePackage == pkg
	}

	String renderHashCodeAndEquals() {
		if (!needsEqualsHashCode()) {
			return ''
		}

		def hashCodeDef = new StringBuilder('\tint hashCode() {')
		hashCodeDef.append newline
		hashCodeDef.append '\t\tdef builder = new HashCodeBuilder()'
		hashCodeDef.append newline

		def equalsDef = new StringBuilder('\tboolean equals(other) {')
		equalsDef.append newline
		equalsDef.append '\t\tif (other == null) return false'
		equalsDef.append newline
		equalsDef.append '\t\tdef builder = new EqualsBuilder()'
		equalsDef.append newline

		getAllPropertiesIterator().each { Property property ->
			if (c2j.getMetaAsBool(property, 'use-in-equals')) {
				String name = findRealIdName(property)
				if (name != property.name) {
					name += '?.id'
				}
				hashCodeDef.append '\t\tbuilder.append '
				hashCodeDef.append name
				hashCodeDef.append newline

				equalsDef.append '\t\tbuilder.append '
				equalsDef.append name
				equalsDef.append ', other.'
				equalsDef.append name
				equalsDef.append newline
			}
		}

		hashCodeDef.append '\t\tbuilder.toHashCode()'
		hashCodeDef.append newline
		hashCodeDef.append '\t}'
		hashCodeDef.append newline

		equalsDef.append '\t\tbuilder.isEquals()'
		equalsDef.append newline
		equalsDef.append '\t}'

		newline + hashCodeDef + newline + equalsDef
	}

	String renderConstraints() {

		def constraints = new StringBuilder()

		getAllPropertiesIterator().each { Property property ->
            if (ignoredProperties.contains(property.name) ||
                (isRefClass() && ignoredPropertiesForRefClass.contains(property.name)) ||
                (isSysClass() && ignoredPropertiesForSysClass.contains(property.name))) {
                return
            }

			if (!getMetaAttribAsBool(property, 'gen-property', true)) {
				return
			}

			def values = [:]
            

//			if (!property.type.isCollectionType() && property.isNullable()) {
//				values.nullable = true
//			}

			if (property.columnSpan == 1) {
				Column column = property.columnIterator.next()
//				if (column.length && column.length != Column.DEFAULT_LENGTH && !isDateType(property.type)) {
//					values.maxSize = column.length
//				}

				if (column.scale != 0 && column.scale != Column.DEFAULT_SCALE) {
					values.scale = column.scale
				}

				if (property != getIdentifierProperty() && column.unique) {
					values.unique = true
				}

				clazz.table.uniqueKeyIterator.each { UniqueKey key ->
					if (key.columnSpan == 1 || key.name == clazz.table.primaryKey.name) return
					if (key.columns[-1] == column) {
						def otherNames = key.columns[0..-2].collect { "\"$it.name\"" }
						values.unique = '[' + otherNames.reverse().join(', ') + ']'
					}
				}
			}

			if (values) {
				constraints.append "\t\t$property.name "
				String delimiter = ''
				values.each { k, v ->
					constraints.append delimiter
					constraints.append "$k: $v"
					delimiter = ', '
				}
				constraints.append newline
			}
		}

		constraints.length() ? "\tstatic constraints = {$newline$constraints\t}" : ''
	}

	protected boolean isDateType(Type type) {
		(type instanceof DateType) || (type instanceof TimestampType) || (type instanceof TimeType) ||
		(type instanceof CalendarType) || (type instanceof CalendarDateType)
	}

	void findNewProperties() {

		def idProperty = getIdentifierProperty()
		def versionProperty = getVersionProperty()

		super.getAllPropertiesIterator().each { Property property ->
			if (property == versionProperty || property == idProperty) {
				return
			}

			if (property.value instanceof ManyToOne) {
				newProperties << property
			}
		}
	}

	String renderMany() {

		def belongs = new TreeSet()
		def hasMany = new TreeSet()
		findBelongsToAndHasMany belongs, hasMany

		if (belongs || hasMany) {
			def many = new StringBuilder()
			if (hasMany) {
				many.append combine('static hasMany = [', ', ', ']', hasMany, true)
				many.append newline
			}
			if (belongs) {
				many.append combine('static belongsTo = [', ', ', ']', belongs)
				many.append newline
			}
			return many.toString()
		}

		''
	}

	String renderMappedBy() {
		def belongs = new TreeSet()
		def hasMany = new TreeSet()
		findBelongsToAndHasMany belongs, hasMany

		if (!hasMany) {
			return ''
		}

		def grouped = [:]
		for (many in hasMany) {
			String[] parts = many.split(':')
			String className = parts[1].trim()
			def propNames = grouped[className]
			if (!propNames) {
				propNames = []
				grouped[className] = propNames
			}
			propNames << parts[0].trim()
		}

		def mappedBy = new TreeSet()
		Set classNames = []
		grouped.each { className, propNames ->
			if (propNames.size() > 1) {
				for (propName in propNames) {
					mappedBy << propName + ': "TODO"'
				}
				classNames << className
			}
		}

		if (!classNames) {
			return ''
		}

		"\t// TODO you have multiple hasMany references for class(es) $classNames " + newline +
		"\t//      so you'll need to disambiguate them with the 'mappedBy' property:" + newline +
		combine('static mappedBy = [', ', ', ']', mappedBy, true) + newline
	}

	protected void findBelongsToAndHasMany(Set belongs, Set hasMany) {

		boolean bidirectionalManyToOne = revengConfig.bidirectionalManyToOne instanceof Boolean ?
				revengConfig.bidirectionalManyToOne : true
		boolean mapManyToManyJoinTable = revengConfig.mapManyToManyJoinTable instanceof Boolean ?
				revengConfig.mapManyToManyJoinTable : false

		def idProperty = getIdentifierProperty()
		def versionProperty = getVersionProperty()
		def strategy = GrailsReverseEngineeringStrategy.INSTANCE

		super.getAllPropertiesIterator().each { Property property ->
			if (property == versionProperty || property == idProperty) {
				return
			}

			if (bidirectionalManyToOne && property.value instanceof ManyToOne) {
				if (!isPartOfPrimaryKey(property)) {
					belongs << classShortName(property.value.referencedEntityName)
				}
			}

			if (property.value instanceof org.hibernate.mapping.Set) {
				boolean isManyToMany = strategy.isManyToManyTable(property.value.collectionTable)
				boolean isReallyManyToMany = strategy.isReallyManyToManyTable(property.value.collectionTable)
				if ((bidirectionalManyToOne && !isManyToMany && !isReallyManyToMany) || isManyToMany) {
					String classShortName = classShortName(property.value.element.type.name)
                    //paulb: fix for Addresses table - 'properties' is not a valid property name
                    if (property.name == 'properties')
                        property.name = 'props'
					hasMany << "$property.name: $classShortName"
					if (isManyToMany) {
						if (strategy.isManyToManyBelongsTo(property.value.collectionTable, property.persistentClass.table)) {
							belongs << findManyToManyOtherSide(property)
						}
					}
				}
			}
		}

		belongs.remove classShortName(getMappedClassName())
	}

	protected boolean isPartOfPrimaryKey(Property prop) {
		if (c2j.isComponent(getIdentifierProperty()) &&
					GrailsReverseEngineeringStrategy.INSTANCE.isReallyManyToManyTable(clazz.table)) {
			String propClassShortName = classShortName(prop.value.referencedEntityName)
			for (newProp in newProperties) {
				if (classShortName(newProp.value.referencedEntityName) == propClassShortName) {
					return true
				}
			}
		}

		false
	}

	protected String findRealIdName(Property prop) {
		if (c2j.isComponent(getIdentifierProperty()) &&
					GrailsReverseEngineeringStrategy.INSTANCE.isReallyManyToManyTable(clazz.table)) {

			for (newProp in newProperties) {
				if (newProp.name + 'Id' == prop.name) {
					return newProp.name
				}
			}
		}

		prop.name
	}

	String renderProperties() {
		def props = new StringBuilder()
		getAllPropertiesIterator().each { Property property ->
			if (getMetaAttribAsBool(property, 'gen-property', true)) {
				if (findRealIdName(property) != property.name ||
                        (ignoredProperties.contains(property.name) ||
                                (isRefClass() && ignoredPropertiesForRefClass.contains(property.name)) ||
                                (isSysClass() && ignoredPropertiesForSysClass.contains(property.name)))) {
                    return
                } else {
                    String javaTypeName = getJavaTypeName(property, true)
                    appendAnnotations(property, javaTypeName, props);
					props.append '\t'
					props.append getJavaTypeName(property, true)
					props.append ' '
					props.append property.name
					props.append newline
				}
			}
		}

		for (prop in newProperties) {
            String javaTypeName = getJavaTypeName(prop, true)
            appendAnnotations(prop, javaTypeName, props);
			props.append '\t'
			props.append classShortName(prop.value.referencedEntityName)
			props.append ' '
			props.append prop.name
			props.append newline
		}

		props.toString()
	}

    void appendAnnotations(Property prop, String javaTypeName, StringBuilder props) {
        StringBuilder annotationBuilder = new StringBuilder()
        if (!prop.type.isCollectionType() && prop.isNullable()) {
            // values.nullable = true
        } else {
            // Special handling for lender_loan_number in HomeLoan - we need to be able to pass an empty string to force
            // the trigger to generate a new loan number
            if (!prop.name.equalsIgnoreCase('lenderLoanNumber') && javaTypeName.equalsIgnoreCase('String')) {
                annotationBuilder.append(' @NotBlank')
            } else {
                annotationBuilder.append(' @NotNull')
            }
        }

        if (prop.columnSpan == 1) {
            Column column = prop.columnIterator.next()

            if (column.length && column.length != Column.DEFAULT_LENGTH &&
                    (!prop.name.toLowerCase().contains('date') || ignoredDateProperties.contains(prop.name)) &&
                    !prop.name.toLowerCase().endsWith('time') &&
                    !prop.name.equalsIgnoreCase('installedOn')) {

                if (javaTypeName.equalsIgnoreCase('String')) {
                    annotationBuilder.append(' @Size(max=' + column.length + ')')
                } else {
                    annotationBuilder.append(' @Max(value=' + column.length + 'L)')
                }
            }
        }

        if (annotationBuilder.length() > 0) {
            // remove first space
            String annotations = annotationBuilder.toString().substring(1);
            props.append '\t'
            props.append annotations
            props.append newline
        }
    }

	String renderMapping() {
		def mapping = new StringBuilder()
		mapping.append renderId()
		mapping.append renderVersion()
		mapping.append renderTable()
		mapping.length() ? "\tstatic mapping = {$newline$mapping\t}" : ''
	}

	String renderClassStart() {
        String declarationName = getDeclarationName()
        String prefix = declarationName.substring(0,3)
        String baseClass = "BaseDomain"
        if (prefix.equals('Ref') && !ignoredRefClasses.contains(declarationName)) {
            baseClass = "BaseRefDomain"
        } else if (prefix.equals('Sys') && !ignoredSysClasses.contains(declarationName)) {
            baseClass = "BaseSysDomain"
        }

        if (declarationName.equalsIgnoreCase('SchemaVersion'))
		    "class ${declarationName} ${renderImplements()}{"
        else
           "class ${declarationName} extends ${baseClass}${renderImplements()}{"
	}

	String renderImplements() {
		getIdentifierProperty().columnSpan > 1 ? ' implements Serializable ' : ' '
	}

	String renderHydraLink() {
		return generateHydraRefClasses.contains(declarationName) ? "\tstatic graphql = com.bss.hydra.graphql.queries.${declarationName}.Query()" : ""
	}

	String getJavaTypeName(Property p, boolean useGenerics) {
		String name = super.getJavaTypeName(p, useGenerics)
        // executionTime & installedOn are from the schema_version table, generated by Flyway
        if (['bestContactStartTime', 'bestContactEndTime'].contains(p.name)) {
            'LocalTime'
        } else if ((p.name.toLowerCase().endsWith('time') && !ignoredTimeProperties.contains(p.name)) || p.name.equalsIgnoreCase('installedOn')) {
            'Instant'
        } else if (p.name.toLowerCase().contains('date') && !ignoredDateProperties.contains(p.name)) {
            'LocalDate'
        } else {
		    typeNameReplacements[name] ?: name
        }
	}

	protected String findManyToManyOtherSide(Property prop) {
		String belongsTo
		prop.value.collectionTable.foreignKeyIterator.each { ForeignKey fk ->
			if (prop.value.table != fk.referencedTable) {
				belongsTo = classShortName(fk.referencedEntityName)
			}
		}
		belongsTo
	}

	protected String classShortName(String className) {
		if (className.indexOf('.') > -1) {
			return className.substring(className.lastIndexOf('.') + 1)
		}
		className
	}

	protected String combine(String start, String delim, String end, things, boolean lineUp = false) {
		def buffer = new StringBuilder('\t')

		String pad
		if (lineUp) {
			def bufferPad = new StringBuilder()
			bufferPad.append newline
			bufferPad.append '\t'
			start.length().times { bufferPad.append ' ' }
			pad = bufferPad.toString()
		}

		buffer.append start
		String delimiter = ''
		things.each {
			buffer.append delimiter
			buffer.append it
			delimiter = lineUp ? delim.trim() + pad : delim
		}
		buffer.append end
		buffer.toString()
	}

    private boolean isRefClass() {
        getDeclarationName().substring(0,3).equals("Ref")
    }

    private boolean isSysClass() {
        getDeclarationName().substring(0,3).equals("Sys")
    }
}
