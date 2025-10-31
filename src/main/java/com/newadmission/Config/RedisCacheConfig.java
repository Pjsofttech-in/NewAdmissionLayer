//package com.newadmission.Config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.springframework.cache.CacheManager;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//
//import java.time.Duration;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Configuration
//@EnableCaching
//public class RedisCacheConfig {
//
//    @Bean
//    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//
//        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(5))
//                .disableCachingNullValues()
//                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
//                        new GenericJackson2JsonRedisSerializer(objectMapper)
//                ));
//        List<String> cacheNames = Arrays.asList(
//                "studentsByClassFilter",
//                "allMediums", "mediumById", "allCourses", "courseById", "passFailCountsCache",
//                "allSourceBy", "sourceById", "allSubjects", "subjectById", "allTeachers", "teacherById", "admissionById",
//                "admissionByDate",  "admissionStatsCache", "dailyAdmissionStatsCache",
//                "monthlyAdmissionStatsCache", "twoYearComparisonStatsCache", "courseStatsCache", "sourceStatsCache", "admissionsByTeacherEmail",
//                "allAdmissions","classRoomById", "allClassRooms","classRoomSubjectDetailsById", "allClassRoomSubjectDetails", "topicNamesByFilters",
//                "studentSubjectResultById", "allStudentSubjectResults", "studentSubjectResultsByStudentId", "teacherStudentResults",
//                "passFailCountsCache","admissionsByClassroomId"
//        );
//
//        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
//        for (String cacheName : cacheNames) {
//            cacheConfigurations.put(cacheName, defaultCacheConfig);
//        }
//
//        return RedisCacheManager.builder(redisConnectionFactory)
//                .cacheDefaults(defaultCacheConfig)
//                .withInitialCacheConfigurations(cacheConfigurations)
//                .build();
//    }
//}