package study.querydsl;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
	
	@Autowired
	EntityManager em;
	
	JPAQueryFactory queryFactory = new JPAQueryFactory(em);
	
	
	@BeforeEach
	public void before() {
		queryFactory = new JPAQueryFactory(em);
		
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
		// member1을 찾아라.
		String qlString = "select m from Member m where m.username = :username";
		Member findMember = em.createQuery(qlString, Member.class)
			.setParameter("username", "member1")
			.getSingleResult();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void startQuerydsl() {
//		JPAQueryFactory queryFactory = new JPAQueryFactory(em);
		queryFactory = new JPAQueryFactory(em);
		
		// 아래보단 아예 'QMember.member'를 static으로 import하여
		// 사용하는 것을 권장
//		QMember m = new QMember("m");
//		QMember m = QMember.member;
		
		Member findMember = queryFactory
			.select(member)
			.from(member)
			.where(member.username.eq("member1"))
			.fetchOne();
		
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void search() {
		queryFactory = new JPAQueryFactory(em);
		
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1")
					.and(member.age.eq(10)))
			.fetchOne();
		
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void searchAndParam() {
		queryFactory = new JPAQueryFactory(em);
		
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1")
					// .and로 체인을 거는 경우
					// ,로 처리할 수도 있다.
					, (member.age.eq(10)))
			.fetchOne();
		
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void resultFetch() {
		queryFactory = new JPAQueryFactory(em);
		
		List<Member> fetch = queryFactory
			.selectFrom(member)
			.fetch();
		
//		Member fetchOne = queryFactory
//			.selectFrom(member)
//			.fetchOne();
//		
//		Member fetchFirst = queryFactory
//			.selectFrom(member)
//			.fetchFirst();
			// '.fetchFirst()'은 아래에 위치한
			// '.limit(1).fetchOne()'와 같다
//			.limit(1).fetchOne();
		
		QueryResults<Member> results = queryFactory
			.selectFrom(member)
			.fetchResults();
		
		results.getTotal();
		List<Member> content = results.getResults();
	}
	
	/**
	 * 회원 정렬 순서
	 * 1. 회원 나이 내림차순(desc)
	 * 2. 회원 이름 올림차순(asc)
	 * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
	 */
	@Test
	public void sort() {
		queryFactory = new JPAQueryFactory(em);
		
		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));
		
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(100))
			.orderBy(member.age.desc(), member.username.asc().nullsLast())
			.fetch();
		
		 Member member5 = result.get(0);
		 Member member6 = result.get(1);
		 Member memberNull = result.get(2);
		 assertThat(member5.getUsername()).isEqualTo("member5");
		 assertThat(member6.getUsername()).isEqualTo("member6");
		 assertThat(memberNull.getUsername()).isNull();
	}
	
	@Test
	public void paging1() {
		queryFactory = new JPAQueryFactory(em);
		List<Member> result = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetch();
		
		assertThat(result.size()).isEqualTo(2);
	}
	
	@Test
	public void paging2() {
		queryFactory = new JPAQueryFactory(em);
		QueryResults<Member> fetchResults = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetchResults();
		
		assertThat(fetchResults.getTotal()).isEqualTo(4);
		assertThat(fetchResults.getLimit()).isEqualTo(2);
		assertThat(fetchResults.getOffset()).isEqualTo(1);
		assertThat(fetchResults.getResults().size()).isEqualTo(2);
	}
	
	@Test
	public void aggregation() {
		queryFactory = new JPAQueryFactory(em);
		
		List<Tuple> result = queryFactory
			.select(
					member.count(),
					member.age.sum(),
					member.age.avg(),
					member.age.max(),
					member.age.min()
					)
			.from(member)
			.fetch();
		
		Tuple tuple = result.get(0);
		assertThat(tuple.get(member.count())).isEqualTo(4);
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		assertThat(tuple.get(member.age.max())).isEqualTo(40);
		assertThat(tuple.get(member.age.min())).isEqualTo(10);
		
	}
	
	
	/**
	 * 팀의 이름과 각 팀의 평균 연령을 구해라.
	 * @throws Exception
	 */
	@Test
	public void group() throws Exception {
		queryFactory = new JPAQueryFactory(em);
		
		List<Tuple> result = queryFactory
			.select(team.name, member.age.avg())
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)
			.fetch();
		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);
		
		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2
		
		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2
	}
	
	@Test
	public void join() {
		queryFactory = new JPAQueryFactory(em);
		
		List<Member> result = queryFactory
			.selectFrom(member)
			.join(member.team, team)
			.where(team.name.eq("teamA"))
			.fetch();
		
		assertThat(result)
			.extracting("username")
			.containsExactly("member1", "member2");
	}
	
	/**
	 * 세타 조인
	 * 회원의 이름이 팀 이름과 같은 회원 조회
	 */
	@Test
	public void theta_join() {
		queryFactory = new JPAQueryFactory(em);
		
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		
		List<Member> result = queryFactory
			.select(member)
			.from(member, team)
			.where(member.username.eq(team.name))
			.fetch();
		
		assertThat(result)
				.extracting("username")
				.containsExactly("teamA", "teamB");
	}
	
	/**
	 * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
	 * JPQL: select m, t from Member m left join m.team t on te.name = 'teamA'
	 */
	@Test
	public void join_on_filtering() {
//		queryFactory = new JPAQueryFactory(em);
		
		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
//			.leftJoin(member.team, team).on(team.name.eq("teamA"))
			
			// 그냥 join 즉, innerJoin 사용 시
			// on 혹은 where을 둘 중 무엇을 사용하든지간에
			// 결과는 같다
			.join(member.team, team)//.on(team.name.eq("teamA"))
			.where(team.name.eq("teamA"))
			.fetch();
		
		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}
	
	/**
	 * 연관관계 없는 엔티티 외부 조인
	 * 회원의 이름이 팀 이름과 같은 대상 외부 조인
	 */
	@Test
	public void join_on_no_relation() {
		queryFactory = new JPAQueryFactory(em);
		
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		
		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(team).on(member.username.eq(team.name))
			.fetch();
		
		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}
	
	@PersistenceUnit
	EntityManagerFactory emf;
	
	@Test
	public void fetchJoinNo() {
		em.flush();
		em.clear();
		
		Member fineMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1"))
			.fetchOne();
			
		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(fineMember.getTeam());
		
		assertThat(loaded).as("패치 조인 미적용").isFalse();
		
	}
	
	@Test
	public void fetchJoinUse() {
		em.flush();
		em.clear();
		
		Member fineMember = queryFactory
			.selectFrom(member)
			.join(member.team, team).fetchJoin()
			.where(member.username.eq("member1"))
			.fetchOne();
			
		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(fineMember.getTeam());
		
		assertThat(loaded).as("패치 조인 미적용").isTrue();
	}
	
	/**
	 * 나이가 가장 많은 회원 조회
	 */
	@Test
	public void subQuery() {
		QMember memberSub = new QMember("memberSub");
		
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(
					select(memberSub.age.max())
							.from(memberSub)
			))
			.fetch();
		
		assertThat(result).extracting("age")
				.containsExactly(40);
		
	}
	
	/**
	 * 나이가 평균 이상인 회원
	 */
	@Test
	public void subQueryGoe() {
		QMember memberSub = new QMember("memberSub");
		
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.goe(
					select(memberSub.age.avg())
							.from(memberSub)
			))
			.fetch();
		
		assertThat(result).extracting("age")
				.containsExactly(30, 40);
		
	}
	
	/**
	 * 나이가 평균 이상인 회원
	 */
	@Test
	public void subQueryIn() {
		QMember memberSub = new QMember("memberSub");
		
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.in(
					select(memberSub.age)
							.from(memberSub)
							.where(memberSub.age.gt(10))
			))
			.fetch();
		
		assertThat(result).extracting("age")
				.containsExactly(20, 30, 40);
	}
	
	@Test
	public void selectSubquery() {
		QMember memberSub = new QMember("memberSub");
		
		List<Tuple> result = queryFactory
				.select(member.username,
						select(memberSub.age.avg())
						 .from(memberSub)) 
				.from(memberSub)
				.fetch();
		
		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
		
	}
	
	@Test
	public void basicCase() {
		List<String> result = queryFactory
				.select(member.age
						.when(10).then("열살")
						.when(20).then("스무살")
						.otherwise("기타"))
				.from(member)
				.fetch();
		
		for (String s : result) {
			System.out.println("s = " + s);
		}
	}
	
	@Test
	public void complexCase() {
		List<String> result = queryFactory
				.select(new CaseBuilder()
							.when(member.age.between(0, 20)).then("0~20살")
							.when(member.age.between(21, 30)).then("21~30살")
							.otherwise("기타"))
				.from(member)
				.fetch();
		
		for (String s : result) {
			System.out.println("s = " + s);
		}
	}
	
	@Test
	public void constant() {
		List<Tuple> result = queryFactory
				.select(member.username, Expressions.constant("A"))
				.from(member)
				.fetch();
		
		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}
	
	@Test
	public void concat() {
		
		// {username}_{age}
		// age가 숫자 타입이기 때문에 오류가 생긴다.
		// 그러나 아래처럼 '.stringValue()'를 써주면 문제를 해결할 수 있다.
		List<String> result = queryFactory
				.select(member.username.concat("_").concat(member.age.stringValue()))
				.from(member)
				.where(member.username.eq("member1"))
				.fetch();
		
		for (String s : result) {
			System.out.println("s = " + s);
		}
	}
	
	@Test
	public void simpleProjection() {
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			.fetch();
		
		for (String s : result) {
			System.out.println("s = " + s);
		}
	}
	
	@Test
	public void tupleProjection() {
		List<Tuple> result = queryFactory
			.select(member.username, member.age)
			.from(member)
			.fetch();
		
		for (Tuple tuple : result) {
			String username = tuple.get(member.username);
			Integer age = tuple.get(member.age);
			System.out.println("username = " + username);
			System.out.println("age = " + age);
		}
	}
	
	@Test
	public void findDtoByJPQL() {
		List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
			.getResultList();
		
		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}
	
	@Test
	// Projections.bean 방식은 setter에 값이 들어감.
	// 즉, DTO에 setter가 없으면 안된다.
	// @Data 어노테이션엔 '@Setter'가 포함되어있음.
	public void findDtoBySetter() {
		List<MemberDto> result = queryFactory
			.select(Projections.bean(MemberDto.class,
					member.username,
					member.age))
			.from(member)
			.fetch();
		
		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}
	
	
	@Test
	// Projections.fields 방식은 해당 필드 변수에 값이 바로 들어감.
	// setter 등이 필요 없다.
	// 그러나 field 명이 서로 일치해야된다.
	public void findDtoByField() {
		List<MemberDto> result = queryFactory
			.select(Projections.fields(MemberDto.class,
					member.username,
					member.age))
			.from(member)
			.fetch();
		
		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}
	
	@Test
	// Projections.fields 방식은 해당 필드 변수에 값이 바로 들어감.
	// setter 등이 필요 없다.
	// 그러나 field 명이 서로 일치해야된다.
	// 만약 일치하지 않는다면 member.username.as("name") 처럼
	// .as를 사용하여 처리해주면 된다.
	public void findUserDto() {
		QMember memberSub = new QMember("memberSub");
		
		List<UserDto> result = queryFactory
			.select(Projections.fields(UserDto.class,
					member.username.as("name"),
					ExpressionUtils.as(JPAExpressions
							.select(memberSub.age.max())
							.from(memberSub), "age"),
					member.age))
			.from(member)
			.fetch();
		
		for (UserDto userDto : result) {
			System.out.println("userDto = " + userDto);
		}
	}
	
	@Test
	// Projections.constructor 방식은 생성자에 넣어준다.
	// setter 등이 필요 없다.
	public void findDtoByConstructor() {
		List<UserDto> result = queryFactory
			.select(Projections.constructor(UserDto.class,
					member.username,
					member.age))
			.from(member)
			.fetch();
		
		for (UserDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

}
