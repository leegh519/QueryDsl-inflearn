package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
@Commit
public class QueryDslBasicTest {

    @PersistenceContext
    private EntityManager em;

    JPAQueryFactory qf;

    @BeforeEach
    public void beforeEach() {
        qf = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        String sqlString = "select m from Member m" +
                " where m.username = :username";
        Member findMember = em.createQuery(sqlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("querydsl")
    public void startQuerydsl() {
        Member findMember = qf.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search() {
        Member findMember = qf.selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    public void searchAndParam() {
        Member findMember = qf.selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        List<Member> fetch = qf.selectFrom(member).fetch();
    }

    @Test
    public void aggregation(){
        List<Tuple> result = qf.select(
                        member.count()
                        , member.age.sum()
                        , member.age.avg()
                        , member.age.max()
                        , member.age.min()
                ).from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
    }

    @Test
    public void group(){
        List<Tuple> result = qf.select(
                        member.team.name
                        , member.age.avg()
                ).from(member)
                .join(member.team , team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
//        Tuple teamB = result.get(1);
        assertThat(teamA.get(member.team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
    }

    @Test
    public void join(){
        List<Member> result = qf.selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result.get(0)).isEqualTo(member);
    }

    @Test
    public void join_on_filtering(){
        List<Tuple> result = qf.select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                // inner join인 경우엔
                // on이나 where이나 차이가 없음
                .on(team.name.eq("teamA"))
                .fetch();

        result.forEach(System.out::println);

    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetch_join(){
        // 영속성 컨텍스트 초기화하지 않을경우
        // 이전에 조회한 엔티티가 남아있어서
        // fetch join을 하지않아도 team이 존재하는 경우가 생김
        em.flush();
        em.clear();

        Member noFetchMember = qf.select(member)
                .from(member)
                .join(member.team, team)
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean noFetch = emf.getPersistenceUnitUtil().isLoaded(noFetchMember.getTeam());

        Member fetchMember = qf.selectFrom(member)
                .join(member.team, team)
                .fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean fetch = emf.getPersistenceUnitUtil().isLoaded(fetchMember.getTeam());

        assertThat(noFetch).as("no fetch join").isFalse();
        assertThat(fetch).as("fetch join").isTrue();


    }

}
