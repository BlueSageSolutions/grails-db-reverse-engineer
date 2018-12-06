${pojo.getPackageDeclaration()}
${pojo.findNewProperties()}
<#assign classbody>
${pojo.renderClassStart()}

${pojo.renderProperties()}

${pojo.renderHashCodeAndEquals()}

${pojo.renderMany()}

${pojo.renderMappedBy()}

${pojo.renderMapping()}

${pojo.renderConstraints()}

${pojo.renderHydraLink()}
}
</#assign>

${pojo.generateImports()}${classbody}