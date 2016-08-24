/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.registries.schemaregistry.client;

import com.hortonworks.registries.schemaregistry.IncompatibleSchemaException;
import com.hortonworks.registries.schemaregistry.InvalidSchemaException;
import com.hortonworks.registries.schemaregistry.SchemaInfo;
import com.hortonworks.registries.schemaregistry.SchemaKey;
import com.hortonworks.registries.schemaregistry.SchemaMetadataKey;
import com.hortonworks.registries.schemaregistry.SchemaNotFoundException;
import com.hortonworks.registries.schemaregistry.SerDesInfo;
import com.hortonworks.registries.schemaregistry.serde.SerDeException;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;

/**
 * This interface defines different methods to interact with remote schema registry.
 * <p>
 * Below code describes how to register new schemas, add new version of a schema and fetch different versions of a schema.
 * <pre>
 *      // registering new schema-metadata
 * SchemaMetadata schemaMetadata = new SchemaMetadata();
 * schemaMetadata.setName("com.hwx.iot.device.schema");
 * schemaMetadata.setSchemaText(schema1);
 * schemaMetadata.setType(type());
 * schemaMetadata.setCompatibility(SchemaProvider.Compatibility.BOTH);
 * SchemaKey schemaKey1 = schemaRegistryClient.registerSchema(schemaMetadata);
 * int v1 = schemaKey1.getVersion();
 *
 * // adding a new version of the schema
 * VersionedSchema schemaInfo2 = new VersionedSchema();
 * schemaInfo2.setSchemaText(schema2);
 * SchemaKey schemaKey2 = schemaRegistryClient.addVersionedSchema(schemaKey1.getId(), schemaInfo2);
 * int v2 = schemaKey2.getVersion();
 *
 * // get the specific version of the schema
 * SchemaDto schemaDto2 = schemaRegistryClient.getSchema(schemaKey2);
 *
 * // get the latest version of the schema
 * SchemaDto latest = schemaRegistryClient.getLatestSchema(schemaKey1.getId());
 * </pre>
 * <p>
 * Below code describes how to register serializer and deserializers, map them with a schema etc.
 * <pre>
 *      // upload a jar containing serializer and deserializer classes.
 * InputStream inputStream = new FileInputStream("/schema-custom-ser-des.jar");
 * String fileId = schemaRegistryClient.uploadFile(inputStream);
 *
 * // add serializer with the respective uploaded jar file id.
 * SerializerInfo serializerInfo = new SerializerInfo();
 * serializerInfo.setName("avro serializer");
 * serializerInfo.setDescription("avro serializer");
 * serializerInfo.setFileId(fileId);
 * serializerInfo.setClassName("con.hwx.iotas.serializer.AvroSnapshotSerializer");
 * Long serializerId = schemaRegistryClient.addSerializer(serializerInfo);
 *
 * // map this serializer with a registered schema
 * schemaRegistryClient.mapSerializer(schemaMetadataId, serializerId);
 *
 * // get registered serializers
 * Collection<SerializerInfo> serializers = schemaRegistryClient.getSerializers(schemaMetadataId);
 * SchemaMetadata schemaMetadata = null;
 * Object input = null;
 *
 * SerializerInfo registeredSerializerInfo = serializers.iterator().next();
 *
 * //get serializer and serialize the given payload
 * try(AvroSnapshotSerializer snapshotSerializer = schemaRegistryClient.createInstance(registeredSerializerInfo);) {
 * Map<String, Object> config = Collections.emptyMap();
 * snapshotSerializer.init(config);
 *
 * byte[] serializedData = snapshotSerializer.serialize(input, schemaMetadata);
 * }
 *
 * </pre>
 */
public interface ISchemaRegistryClient extends AutoCloseable {

    /**
     * Returns true if the given {@code schemaMetadata} is successfully registered.
     *
     * @param schemaMetadata
     * @return
     */
    boolean registerSchemaMetadata(SchemaMetadata schemaMetadata) throws InvalidSchemaException;

    /**
     * Returns version of the schema added to schema-metadata.
     * <pre>
     * It tries to fetch an existing schema or register the given schema with the below conditions
     *  - Checks whether there exists a schema with the given schemaText, and schemaMetadata
     *      - returns respective schemaKey if it exists.
     *      - Creates a schema for the given name and returns respective schemaKey if it does not exist
     * </pre>
     *
     * @param schemaMetadata
     * @param versionedSchema
     * @return
     * @throws InvalidSchemaException
     */
    Integer registerSchema(SchemaMetadata schemaMetadata, VersionedSchema versionedSchema) throws InvalidSchemaException;

    /**
     * Returns {@link SchemaKey} after adding the given schema as the next version of the schema.
     *
     * @param schemaMetadataId
     * @param versionedSchema
     * @return
     * @throws InvalidSchemaException      if the given schema is not valid.
     * @throws IncompatibleSchemaException if the given schema is incompatible according to the compatibility set.
     */
    SchemaKey addVersionedSchema(Long schemaMetadataId, VersionedSchema versionedSchema) throws InvalidSchemaException, IncompatibleSchemaException;

    /**
     * Returns {@link SchemaKey} after adding the given schema as the next version of the schema.
     *
     * @param schemaMetadataKey
     * @param versionedSchema
     * @return
     * @throws InvalidSchemaException      if the given schema is not valid.
     * @throws IncompatibleSchemaException if the given schema is incompatible according to the compatibility set.
     */
    Integer addVersionedSchema(SchemaMetadataKey schemaMetadataKey, VersionedSchema versionedSchema) throws InvalidSchemaException, IncompatibleSchemaException;

    /**
     * Returns all schemas registered in the repository. It may be paging the results internally with out realizing all
     * the results.
     *
     * @return
     */
    Iterable<SchemaInfo> listAllSchemas();

    /**
     * Returns {@link SchemaInfo} for the given {@link SchemaKey}
     *
     * @param schemaKey
     * @return
     */
    SchemaInfo getSchema(SchemaKey schemaKey) throws SchemaNotFoundException;

    /**
     * Returns the latest version of the schema for the given {@param schemaMetadataId}
     *
     * @param schemaMetadataId
     * @return
     */
    SchemaInfo getLatestSchema(Long schemaMetadataId) throws SchemaNotFoundException;


    /**
     * Returns the latest version of the schema for the given {@param schemaMetadataKey}
     *
     * @param schemaMetadataKey
     * @return
     */
    SchemaInfo getLatestSchema(SchemaMetadataKey schemaMetadataKey) throws SchemaNotFoundException;

    /**
     * Returns all versions of the schemas for given {@param schemaMetadataId}
     *
     * @param schemaMetadataId
     * @return
     */
    Iterable<SchemaInfo> getAllVersions(Long schemaMetadataId) throws SchemaNotFoundException;

    /**
     * Returns all versions of the schemas for given {@param schemaMetadataKey}
     *
     * @param schemaMetadataKey
     * @return
     */
    Collection<SchemaInfo> getAllVersions(SchemaMetadataKey schemaMetadataKey) throws SchemaNotFoundException;

    /**
     * Returns true if the given {@code toSchemaText} is compatible with the latest version of the schema with id as {@code schemaMetadataId}.
     *
     * @param schemaMetadataId
     * @param toSchemaText
     * @return
     */
    boolean isCompatibleWithAllVersions(Long schemaMetadataId, String toSchemaText) throws SchemaNotFoundException;

    /**
     * Returns true if the given {@code toSchemaText} is compatible with the latest version of the schema with id as {@code schemaMetadataKey}.
     *
     * @param schemaMetadataKey
     * @param toSchemaText
     * @return
     */
    boolean isCompatibleWithAllVersions(SchemaMetadataKey schemaMetadataKey, String toSchemaText) throws SchemaNotFoundException;

    /**
     * Returns unique id for the uploaded bytes read from input stream to file storage.
     *
     * @param inputStream
     * @return
     */
    String uploadFile(InputStream inputStream) throws SerDeException;

    /**
     * Downloads the content of file stored with the given {@code fileId}.
     *
     * @param fileId
     * @return
     */
    InputStream downloadFile(String fileId) throws FileNotFoundException;

    /**
     * Returns unique id for the added Serializer for the given {@code schemaSerializerInfo}
     *
     * @param serializerInfo
     * @return
     */
    Long addSerializer(SerDesInfo serializerInfo);

    /**
     * Returns unique id for the added Serializer for the given {@code schemaSerializerInfo}
     *
     * @param deserializerInfo
     * @return
     */
    Long addDeserializer(SerDesInfo deserializerInfo);

    /**
     * Maps Serializer/Deserializer of the given {@code serDesId} to Schema with {@code schemaMetadataId}
     *
     * @param schemaMetadataId
     * @param serializerId
     */
    void mapSchemaWithSerDes(Long schemaMetadataId, Long serializerId) throws SchemaNotFoundException;

    /**
     * Maps Serializer/Deserializer of the given {@code serDesId} to Schema with {@code schemaMetadataKey}
     *
     * @param schemaMetadataKey
     * @param serDesId
     */
    void mapSchemaWithSerDes(SchemaMetadataKey schemaMetadataKey, Long serDesId);

    /**
     * Returns Collection of Serializers registered for the schema with {@code schemaMetadataId}
     *
     * @param schemaMetadataId
     * @return
     */
    Collection<SerDesInfo> getSerializers(Long schemaMetadataId) throws SchemaNotFoundException;

    /**
     * Returns Collection of Serializers registered for the schema with {@code schemaMetadataKey}
     *
     * @param schemaMetadataKey
     * @return
     */
    Collection<SerDesInfo> getSerializers(SchemaMetadataKey schemaMetadataKey);

    /**
     * Returns Collection of Deserializers registered for the schema with {@code schemaMetadataId}
     *
     * @param schemaMetadataId
     * @return
     */
    Collection<SerDesInfo> getDeserializers(Long schemaMetadataId) throws SchemaNotFoundException;

    /**
     * Returns Collection of Deserializers registered for the schema with {@code schemaMetadataKey}
     *
     * @param schemaMetadataKey
     * @return
     */
    Collection<SerDesInfo> getDeserializers(SchemaMetadataKey schemaMetadataKey);

    /**
     * Returns a new instance of the respective Serializer class for the given {@code serializerInfo}
     *
     * @param <T>            type of the instance to be created
     * @param serializerInfo
     * @throws SerDeException throws an Exception if serializer or deserializer class is not an instance of {@code T}
     */
    <T> T createSerializerInstance(SerDesInfo serializerInfo);

    /**
     * Returns a new instance of the respective Deserializer class for the given {@code deserializerInfo}
     *
     * @param <T>              type of the instance to be created
     * @param deserializerInfo
     * @throws SerDeException throws an Exception if serializer or deserializer class is not an instance of {@code T}
     */
    <T> T createDeserializerInstance(SerDesInfo deserializerInfo);

}