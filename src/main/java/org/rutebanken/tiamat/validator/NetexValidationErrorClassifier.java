package org.rutebanken.tiamat.validator;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.tiamat.general.NetexLineLocator;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.config.Messages.*;

public class NetexValidationErrorClassifier {

    private static final Set<String> TEMPLATE_ERROR_CODES = Set.of(
            VALIDATION_TOF_REQUIRED,
            VALIDATION_TOF_ID_INVALID,
            VALIDATION_TOF_VERSION_INVALID,
            VALIDATION_TOF_NAME_INVALID,
            VALIDATION_TOF_DESCRIPTION_INVALID,
            VALIDATION_MUST_NOT_CONTAIN_REFS,
            VALIDATION_MAXIMUM_PARKING_DEPTH_EXCEEDED,
            VALIDATION_MAXIMUM_PARKING_PROPERTIES_EXCEEDED
    );

    private NetexValidationErrorClassifier() {
    }

    public static Set<AnalyzeImportErrorType> classify(BindException bindException) {
        Set<AnalyzeImportErrorType> errorTypes = new LinkedHashSet<>();
        bindException.getAllErrors().forEach(error -> errorTypes.add(classify(error)));
        return errorTypes;
    }

    /**
     * Invalid id format and missing data errors are reported one by one with their details: field, type and id
     * of the entity holding the field, line of this entity in the file and, for invalid ids, the invalid value.
     * The other errors are only reported once, without params.
     *
     * @param bindException validation errors of the imported publication delivery
     * @param lineLocator   locator of the entities in the imported file, may be null
     * @return the job errors
     */
    public static List<JobError> toJobErrors(BindException bindException, NetexLineLocator lineLocator) {
        Set<JobError> jobErrors = new LinkedHashSet<>();
        for (ObjectError error : bindException.getAllErrors()) {
            AnalyzeImportErrorType type = classify(error);
            if (type == AnalyzeImportErrorType.INVALID_FORMAT_ID || type == AnalyzeImportErrorType.MISSING_DATA) {
                jobErrors.add(toDetailedJobError(type, bindException, error, lineLocator));
            } else {
                jobErrors.add(JobError.error(type));
            }
        }
        return new ArrayList<>(jobErrors);
    }

    private static AnalyzeImportErrorType classify(ObjectError error) {
        if (VALIDATION_INVALID_ID_FORMAT.equals(error.getCode())) {
            return AnalyzeImportErrorType.INVALID_FORMAT_ID;
        }
        if (TEMPLATE_ERROR_CODES.contains(error.getCode())) {
            return AnalyzeImportErrorType.TEMPLATE;
        }
        return AnalyzeImportErrorType.MISSING_DATA;
    }

    private static JobError toDetailedJobError(AnalyzeImportErrorType type, BindException bindException, ObjectError error, NetexLineLocator lineLocator) {
        JobError jobError = JobError.error(type);
        FieldLocation location = error instanceof FieldError fieldError ? FieldLocation.of(fieldError.getField()) : null;
        Object entity = location != null ? getRawFieldValue(bindException, location.entityPath()) : null;

        String netexField = location != null ? location.netexField() : null;
        if (netexField != null) {
            jobError.withParam("field", netexField);
        }
        if (entity != null) {
            jobError.withParam("entity", netexTypeName(entity));
        }
        String entityId = entity instanceof EntityStructure entityStructure && entityStructure.getId() != null
                ? entityStructure.getId()
                : firstArgument(error);
        if (entityId != null) {
            jobError.withParam("entityId", entityId);
        }
        Integer line = lineLocator != null ? lineLocator.findIdLine(entityId) : null;
        if (line != null) {
            jobError.withParam("line", String.valueOf(line));
        }
        if (type == AnalyzeImportErrorType.INVALID_FORMAT_ID) {
            jobError.withParam("errorValue", entityId != null ? entityId : "");
        }
        return jobError;
    }

    private static Object getRawFieldValue(BindException bindException, String path) {
        if (StringUtils.isEmpty(path)) {
            return null;
        }
        try {
            return bindException.getBindingResult().getRawFieldValue(path);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String firstArgument(ObjectError error) {
        return error.getArguments() != null && error.getArguments().length > 0 && error.getArguments()[0] != null
                ? error.getArguments()[0].toString()
                : null;
    }

    /**
     * JAXB classes of NeTEx child structures carry a suffix, e.g. ParkingProperties_VersionedChildStructure.
     */
    private static String netexTypeName(Object entity) {
        return StringUtils.substringBefore(entity.getClass().getSimpleName(), "_");
    }

    /**
     * Splits a validation field path such as
     * {@code dataObjects.compositeFrameOrCommonFrame[0].value.members.generalFrameMemberOrDataManagedObjectOrEntity_Entity[2].value.centroid.location.longitude}
     * into the path of the entity holding the field (everything up to the last indexed element) and the field relative to this entity.
     */
    record FieldLocation(String entityPath, String relativeField) {

        static FieldLocation of(String fieldPath) {
            if (fieldPath == null) {
                return null;
            }
            int lastIndex = fieldPath.lastIndexOf(']');
            if (lastIndex < 0) {
                return new FieldLocation("", fieldPath);
            }
            String entityPath = fieldPath.substring(0, lastIndex + 1);
            String rest = StringUtils.removeStart(fieldPath.substring(lastIndex + 1), ".");
            if (rest.equals("value") || rest.startsWith("value.")) {
                entityPath += ".value";
                rest = StringUtils.removeStart(StringUtils.removeStart(rest, "value"), ".");
            }
            return new FieldLocation(entityPath, rest);
        }

        /**
         * @return the field as a path of NeTEx elements, e.g. Centroid/Location/Longitude, or null if there is no field
         */
        String netexField() {
            if (StringUtils.isEmpty(relativeField)) {
                return null;
            }
            if (relativeField.equals("id")) {
                return "id";
            }
            List<String> segments = new ArrayList<>(Arrays.asList(relativeField.split("\\.")));
            // JAXB text and reference attributes are not NeTEx elements: name.value -> Name, siteRef.ref -> SiteRef
            if (segments.size() > 1 && (segments.getLast().equals("value") || segments.getLast().equals("ref"))) {
                segments.removeLast();
            }
            return segments.stream()
                    .map(StringUtils::capitalize)
                    .collect(Collectors.joining("/"));
        }
    }
}
