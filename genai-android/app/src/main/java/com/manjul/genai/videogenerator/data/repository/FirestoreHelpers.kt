package com.manjul.genai.videogenerator.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.manjul.genai.videogenerator.data.model.CategorizedParameters
import com.manjul.genai.videogenerator.data.model.ModelSchemaMetadata
import com.manjul.genai.videogenerator.data.model.ParameterType
import com.manjul.genai.videogenerator.data.model.SchemaParameter

fun DocumentSnapshot.getStringList(field: String): List<String> {
    val raw = get(field)
    return (raw as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
}

fun DocumentSnapshot.getNumberList(field: String): List<Int> {
    val raw = get(field)
    return (raw as? List<*>)?.mapNotNull {
        when (it) {
            is Number -> it.toInt()
            is String -> it.toIntOrNull()
            else -> null
        }
    } ?: emptyList()
}

fun parseSchemaParameters(data: Any?): List<SchemaParameter> {
    if (data == null) return emptyList()
    val list = data as? List<Map<String, Any?>> ?: return emptyList()
    return list.mapNotNull { paramMap ->
        try {
            SchemaParameter(
                name = paramMap["name"] as? String ?: return@mapNotNull null,
                type = parseParameterType(paramMap["type"] as? String),
                required = paramMap["required"] as? Boolean ?: false,
                nullable = paramMap["nullable"] as? Boolean ?: false,
                description = paramMap["description"] as? String,
                defaultValue = paramMap["default"],
                enumValues = (paramMap["enum"] as? List<*>)?.mapNotNull { it },
                min = (paramMap["min"] as? Number)?.toDouble(),
                max = (paramMap["max"] as? Number)?.toDouble(),
                format = paramMap["format"] as? String,
                title = paramMap["title"] as? String,
            )
        } catch (e: Exception) {
            null
        }
    }
}

fun parseParameterType(type: String?): ParameterType {
    return when (type?.lowercase()) {
        "number", "integer" -> ParameterType.NUMBER
        "boolean" -> ParameterType.BOOLEAN
        "array" -> ParameterType.ARRAY
        "object" -> ParameterType.OBJECT
        "enum" -> ParameterType.ENUM
        else -> ParameterType.STRING
    }
}

fun parseSchemaMetadata(data: Any?): ModelSchemaMetadata? {
    if (data == null) return null
    val map = data as? Map<String, Any?> ?: return null
    return try {
        val requiredFields = (map["required_fields"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val categorized = map["categorized"] as? Map<String, Any?> ?: return null
        
        ModelSchemaMetadata(
            requiredFields = requiredFields,
            categorized = CategorizedParameters(
                text = parseSchemaParameters(categorized["text"]),
                numeric = parseSchemaParameters(categorized["numeric"]),
                boolean = parseSchemaParameters(categorized["boolean"]),
                enum = parseSchemaParameters(categorized["enum"]),
                file = parseSchemaParameters(categorized["file"]),
            )
        )
    } catch (e: Exception) {
        null
    }
}

