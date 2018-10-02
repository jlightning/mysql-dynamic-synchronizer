package com.jhl.mds.jsclientgenerator;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JsJsDTOClassGenerator extends JsDTOGenerator {

    private TemplateReader templateReader;
    private JsJsDTOEnumGenerator jsDTOEnumGenerator;
    private TypeCommentGenerator typeCommentGenerator;
    private DTORegistry dtoRegistry;
    private FileUtils fileUtils;
    private Map<Class, String> generated = new HashMap<>();
    private Class processing = null;

    public JsJsDTOClassGenerator(TemplateReader templateReader, JsJsDTOEnumGenerator jsDTOEnumGenerator, TypeCommentGenerator typeCommentGenerator, DTORegistry dtoRegistry, FileUtils fileUtils) {
        super(templateReader);
        this.templateReader = templateReader;
        this.jsDTOEnumGenerator = jsDTOEnumGenerator;
        this.typeCommentGenerator = typeCommentGenerator;
        this.dtoRegistry = dtoRegistry;
        this.fileUtils = fileUtils;
        typeCommentGenerator.setJsDTOClassGenerator(this);
    }

    public void start() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(JsClientDTO.class));

        for (BeanDefinition bd : scanner.findCandidateComponents("com.jhl.mds")) {
            Class<?> clazz = Class.forName(bd.getBeanClassName());

            generateDto(clazz, null);
        }
    }

    public String generateDto(Class<?> clazz, String appendToFileIfAnnotationNotFound) throws IOException {
        if (clazz.isEnum()) return jsDTOEnumGenerator.generateDto(clazz, appendToFileIfAnnotationNotFound);

        if (generated.containsKey(clazz)) return generated.get(clazz);
        JsClientDTO jsClientDTO = clazz.getAnnotation(JsClientDTO.class);

        Pair<String, String> pair;
        try {
            pair = getFileNameAndClassName(jsClientDTO, clazz, appendToFileIfAnnotationNotFound);
        } catch (Exception e) {
            return "";
        }

        String fileName = pair.getFirst();
        String className = pair.getSecond();

        if (clazz == processing) return className;
        processing = clazz;

        fileUtils.initClean(BASE_CLIENT_JS_DIRECTORY + fileName + ".js");

        List<BeanPropertyDefinition> properties = getDTOproperties(clazz);

        Field[] fields = clazz.getDeclaredFields();
        List<String> fieldStr = new ArrayList<>();
        List<String> constructorParameters = new ArrayList<>();
        List<String> constructorSetters = new ArrayList<>();
        for (Field field : fields) {
            String type = typeCommentGenerator.getFieldTypeComment(field, fileName);

            String renderedField = templateReader.getDtoFieldTemplate().replaceAll("\\{field}", getFieldName(field, properties) + ": ?" + type);

            renderedField = renderedField.replaceAll("\\{type}", type);
            renderedField = renderedField.replaceAll("\\{default_value}", getDefaultValueForField(field));
            fieldStr.add(renderedField);

            constructorParameters.add(getFieldName(field, properties) + ": ?" + type);

            constructorSetters.add(templateReader.getDtoConstructorSetterTemplate().replaceAll("\\{parameter}", getFieldName(field, properties)));
        }

        String renderedClass = renderClass(className, fieldStr, constructorParameters, constructorSetters, typeCommentGenerator.renderMethodComment(fields, fileName), "");

        fileUtils.append(BASE_CLIENT_JS_DIRECTORY + fileName + ".js", renderedClass);

        generated.put(clazz, className);
        processing = null;

        dtoRegistry.addTmpGenerated(new DTORegistry.GeneratedDefinition(className, BASE_CLIENT_JS_DIRECTORY + fileName + ".js"));
        return className;
    }

    private List<BeanPropertyDefinition> getDTOproperties(Class clazz) {
        ObjectMapper mapper = new ObjectMapper();

        JavaType type = mapper.getTypeFactory().constructType(clazz);
        BeanDescription introspection =
                mapper.getSerializationConfig().introspect(type);
        List<BeanPropertyDefinition> properties = introspection.findProperties();

        return properties;
    }

    private String getDefaultValueForField(Field field) {
        String defaultValue = "null";
        if (field.getType().isPrimitive()) {
            defaultValue = "0";
            if (field.getType() == boolean.class) {
                defaultValue = "false";
            } else if (field.getType() == char.class) {
                defaultValue = "''";
            }
        } else if (field.getType().getName().equals("java.lang.String")) {
            return "''";
        }
        return defaultValue;
    }

    private String getFieldName(Field field, List<BeanPropertyDefinition> properties) {
        for (BeanPropertyDefinition property : properties) {
            if (property.getGetter() != null && property.getGetter().getName().equals(field.getName())) {
                return property.getFullName().getSimpleName();
            }
        }
        return field.getName();
    }
}
