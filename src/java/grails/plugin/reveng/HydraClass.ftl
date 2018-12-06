package com.bss.hydra.graphql.queries

import grails.gorm.DetachedCriteria
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.EntityDataFetcher

<#assign classbody>
${pojo.renderClassStart()}

    def static Query() {
    }
}
</#assign>

${classbody}