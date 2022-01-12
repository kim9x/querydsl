package study.querydsl.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 'lombok'의'@ToString'을 사용하려면 연관 필드인  team 같은 것은 사용하면 안된다.
// 왜냐하면 team에서 member 갔다가 member에서 있으면
// 또 team을 오고 하기 때문에 무한 루프에 빠질 수 있다.
@ToString(of = {"id", "name"})
public class Team {
	
	@Id @GeneratedValue
	@Column(name = "team_id")
	private Long id;
	private String name;
	
	// �ϳ��� ���� ���� ����� �� �� �ִ�.
	@OneToMany(mappedBy = "team")
	private List<Member> members = new ArrayList<>();

	public Team(String name) {
		super();
		this.name = name;
	}

}
