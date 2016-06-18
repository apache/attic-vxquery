/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.vxquery.xmlquery.ast;

public enum ASTTag {
    ARGUMENT_LIST,
    VERSION_DECL,
    PROLOG,
    LIBRARY_MODULE,
    MODULE_DECLARATION,
    MAIN_MODULE,
    QUERY_BODY,
    BOUNDARY_SPACE_DECLARATION,
    DEFAULT_ELEMENT_NAMESPACE_DECLARATION,
    DEFAULT_FUNCTION_NAMESPACE_DECLARATION,
    OPTION_DECLARATION,
    FT_OPTION_DECLARATION,
    ORDERING_MODE_DECLARATION,
    EMPTY_ORDER_DECLARATION,
    COPY_NAMESPACES_DECLARATION,
    DEFAULT_COLLATION_DECLARATION,
    BASE_URI_DECLARATION,
    SCHEMA_IMPORT,
    MODULE_IMPORT,
    VARIABLE_DECLARATION,
    TYPE_DECLARATION,
    SEQUENCE_TYPE,
    ITEM_TYPE,
    ATOMIC_TYPE,
    ANY_NODE_TEST,
    ITEM_TEST,
    DOCUMENT_TEST,
    QNAME,
    ELEMENT_TEST,
    SCHEMA_ELEMENT_TEST,
    ATTRIBUTE_TEST,
    SCHEMA_ATTRIBUTE_TEST,
    TEXT_TEST,
    COMMENT_TEST,
    PI_TEST,
    TYPE_NAME,
    CONSTRUCTION_DECLARATION,
    FUNCTION_DECLARATION,
    FUNCTION_PARAMETER,
    ENCLOSED_EXPRESSION,
    EXPRESSION,
    FLWOR_EXPRESSION,
    FOR_CLAUSE,
    FOR_VARIABLE_DECLARATION,
    LET_VARIABLE_DECLARATION,
    LET_CLAUSE,
    WHERE_CLAUSE,
    ORDERBY_CLAUSE,
    ORDER_SPECIFICATION,
    QUANTIFIED_EXPRESSION,
    QUANTIFIED_VARIABLE_DECLARATION,
    TYPESWITCH_EXPRESSION,
    CASE_CLAUSE,
    IF_EXPRESSION,
    INFIX_EXPRESSION,
    TYPE_EXPRESSION,
    UNARY_EXPRESSION,
    VALIDATE_EXPRESSION,
    EXTENSION_EXPRESSION,
    PRAGMA_NODE,
    PATH_EXPRESSION,
    AXIS_STEP,
    NAME_TEST,
    POSTFIX_EXPRESSION,
    LITERAL,
    VARIABLE_REFERENCE,
    PARENTHESIZED_EXPRESSION,
    CONTEXT_ITEM,
    ORDERED_EXPRESSION,
    UNORDERED_EXPRESSION,
    FUNCTION_EXPRESSION,
    DIRECT_ELEMENT_CONSTRUCTOR,
    DIRECT_ATTRIBUTE_CONSTRUCTOR,
    DQUOTED_ATTRIBUTE_CONTENT,
    SQUOTED_ATTRIBUTE_CONTENT,
    TEXTUAL_NODE_CONTENT,
    CDATA_SECTION,
    DIRECT_COMMENT_CONSTRUCTOR,
    DIRECT_PI_CONSTRUCTOR,
    COMPUTED_DOCUMENT_CONSTRUCTOR,
    COMPUTED_ATTRIBUTE_CONSTRUCTOR,
    COMPUTED_ELEMENT_CONSTRUCTOR,
    COMPUTED_TEXT_CONSTRUCTOR,
    COMPUTED_COMMENT_CONSTRUCTOR,
    EMPTY_SEQUENCE_TYPE,
    RELATIVE_PATH_EXPRESSION,
    COMPUTED_PI_CONSTRUCTOR,
    NCNAME,
    CONTENT_CHARS,
    NAMESPACE_DECLARATION,
    SINGLE_TYPE,
    ARRAY_CONSTRUCTOR,
    OBJECT_CONSTRUCTOR,
    PAIR_CONSTRUCTOR
}
