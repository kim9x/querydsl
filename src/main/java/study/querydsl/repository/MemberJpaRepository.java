package study.querydsl.repository;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

@Repository
public class MemberJpaRepository {
	
	private final EntityManager em;
	private final JPAQueryFactory queryFactory;
	
	public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
		this.em = em;
//		this.queryFactory = new JPAQueryFactory(em);
		
		// JPAQueryFactory가 bean으로 등록되어 있기 때문에 밑에처럼 해도 된다.   
		this.queryFactory = queryFactory;
		
	}
	
	public void save(Member member) {
		em.persist(member);
	}
	
	public Optional<Member> findById(Long id) {
		Member findMember = em.find(Member.class, id);
		return Optional.ofNullable(findMember);
	}
	
	public List<Member> findAll() {
		return em.createQuery("select m from Member m", Member.class)
				.getResultList();
	}
	
	public List<Member> findAll_Querydsl() {
		return queryFactory
				.selectFrom(member)
				.fetch();
	}
	
	public List<Member> findByUsername(String username) {
		return em.createQuery("select m from Member m where m.username = :username", Member.class)
				.setParameter("username", username)
				.getResultList();
	}
	
	public List<Member> findByUsername_Querydsl(String username) {
		return queryFactory
				.selectFrom(member)
				.where(member.username.eq(username))
				.fetch();
	}
	
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
    	
    	BooleanBuilder builder = new BooleanBuilder();
    	if (hasText(condition.getUsername())) {
    		builder.and(member.username.eq(condition.getUsername()));
    	}
    	if (hasText(condition.getTeamName())) {
    		builder.and(team.name.eq(condition.getTeamName()));
    	}
    	if (condition.getAgeGoe() != null) {
    		builder.and(member.age.goe(condition.getAgeGoe()));
    	}
    	if (condition.getAgeLoe() != null) {
    		builder.and(member.age.loe(condition.getAgeLoe()));
    	}
    	
        return queryFactory
                .select(new QMemberTeamDto(
                		member.id.as("memberId"),
                		member.username,
                		member.age,
                		team.id.as("teamId"),
                		team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
                
                
    }
	
	

}
