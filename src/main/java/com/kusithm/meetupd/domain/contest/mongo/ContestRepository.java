package com.kusithm.meetupd.domain.contest.mongo;

import com.kusithm.meetupd.domain.contest.entity.Contest;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContestRepository extends MongoRepository<Contest, String> {

    @Query(value = "{recruit_end : {$gte : ?0}}", sort = "{ recruit_end : 1}")
    List<Contest> findAllContestsByDate(LocalDate date);

    @Query(value = "{recruit_end : {$gte : ?0}, types :  {$eq : ?1}}", sort = "{ recruit_end : 1}")
    List<Contest> findContestsByDateAndType(LocalDate date, Integer num);

    @Query(value = "{_id : {$eq : ?0}}")
    Optional<Contest> findContestById(ObjectId contestId);

    @Query(value = "{recruit_end : {$gte : ?0, $lt : ?1}}")
    List<Contest> findAllEndContestsToday(LocalDate start, LocalDate end);


    @Aggregation(pipeline = {
            "{ $match: { recruit_end: { $gte: ?0 } } }",
            "{ '$sort' : { 'team_num' : -1 } }",
            "{ '$limit' : 6 }"
    })
    List<Contest> findRecommendationSixContests(LocalDate date);
}
