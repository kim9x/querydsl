package study.querydsl.repository;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;

import javax.persistence.EntityManager;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

// 특정 기능에 특화된 쿼리의 경우
// 별로 repository로 따로 분리해서
// 주입해줘서 아래처럼 사용해줘도 좋다.
public class MemberQueryRepository {
	
	private final JPAQueryFactory queryFactory;
	
	public MemberQueryRepository(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
	}
	
	public List<MemberTeamDto> search(MemberSearchCondition condition) {
    	return queryFactory
    			.select(new QMemberTeamDto(
                		member.id.as("memberId"),
                		member.username,
                		member.age,
                		team.id.as("teamId"),
                		team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                		usernameEq(condition.getUsername()),
                		teamNameEq(condition.getTeamName()),
                		ageGoe(condition.getAgeGoe()),
                		ageLoe(condition.getAgeLoe()))
                .fetch();
	}
	
	private BooleanExpression usernameEq(String username) {
		return hasText(username) ? member.username.eq(username) : null;
	}

	private BooleanExpression teamNameEq(String teamName) {
		return hasText(teamName) ? team.name.eq(teamName) : null;
	}

	private BooleanExpression ageGoe(Integer ageGoe) {
		return ageGoe != null ? member.age.goe(ageGoe) : null;
	}

	private BooleanExpression ageLoe(Integer ageLoe) {
		return ageLoe != null ? member.age.loe(ageLoe) : null;
	}
}
