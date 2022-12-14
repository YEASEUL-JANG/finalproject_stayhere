package com.example.stayhere.service.host;

import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.mail.HtmlEmail;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.stayhere.model.guest.dto.GuestDTO;
import com.example.stayhere.model.host.dao.HostDAO;
import com.example.stayhere.model.host.dto.HostDTO;
import com.example.stayhere.model.reservations.dto.ReservationsDTO;
import com.example.stayhere.model.rooms.dto.RoomsDTO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HostServiceImpl implements HostService {

	@Inject
	HostDAO hostDao;

	@Inject
	HostService hostService;
	
	@Inject
	BCryptPasswordEncoder pwdEncoder;
	
	@Override
	public boolean loginCheck(HostDTO dto, HttpSession session) {
		boolean result = hostDao.loginCheck(dto);
		if(result) { //로그인 성공시 세션에 저장
			session.setAttribute("h_userid", dto.getH_userid());
			session.setAttribute("h_name", dto.getH_name());
		}
		return result;
	}
	
	@Override
	public HostDTO loginOkNick(HostDTO dto) {
		return hostDao.loginOkNick(dto);
	}

	@Override
	public HostDTO viewHost(String h_userid) {
		return hostDao.viewHost(h_userid);
	}

	@Override
	public void logout(HttpSession session) {
		session.invalidate();
	}

	@Override
	public void insert(HostDTO host) {
		hostDao.insert(host);
	}
	
	@Override
	public void sendEmail(HostDTO dto, String div, String num) throws Exception {
		// Mail Server 설정
		String charSet = "utf-8";
		String hostSMTP = "smtp.naver.com"; //구글 이용시 smtp.gmail.com
		String hostSMTPid = "project-test-user@naver.com";
		String hostSMTPpwd = "project!test";

		// 보내는 사람 EMail, 제목, 내용
		String fromEmail = "project-test-user@naver.com";
		String fromName = "stayhere 관리자";
		String subject = "";
		String msg = "";
		
		if(div.equals("findpw")) {
			subject = "STAYHERE 임시 비밀번호 입니다.";
			msg += "<div align='center' style='border:1px solid black; font-family:verdana'>";
			msg += "<h3 style='color: blue;'>";
			msg += dto.getH_passwd() + "님의 임시 비밀번호 입니다. 비밀번호를 변경하여 사용하세요.</h3>";
			msg += "<p>임시 비밀번호 : ";
			msg += dto.getH_passwd() + "</p></div>";
		} else if(div.equals("auth")) {
			System.out.println("메일 인증 발송 직전!");
			subject = "STAYHERE 인증 코드입니다.";
			msg += "<div align='center' style='border:1px solid black; font-family:verdana'>";
			msg += "<h3 style='color: blue;'>";
			msg += "[인증번호] " + num + "입니다. <br>인증번호 확인란에 기입하여 주십시오.</h3></div>";
		}

		// 받는 사람 E-Mail 주소
		String mail = dto.getH_email();
		try {
			HtmlEmail email = new HtmlEmail();
			email.setDebug(true);
			email.setCharset(charSet);
			//email.setSSL(true);
			email.setSSLOnConnect(true);
			email.setHostName(hostSMTP);
			email.setSmtpPort(465); //구글 이용시 465
			email.setAuthentication(hostSMTPid, hostSMTPpwd);
			//email.setTLS(true);
			email.setStartTLSEnabled(true);
			email.addTo(mail, charSet);
			email.setFrom(fromEmail, fromName, charSet);
			email.setSubject(subject);
			email.setHtmlMsg(msg);
			email.send();
		} catch (Exception e) {
			System.out.println("메일발송 실패 : " + e);
		}
		
	}
	
	@Override
	public void findPw(HttpServletResponse response, HostDTO dto) throws Exception {
		response.setContentType("text/html;charset=utf-8");
		HostDTO ck = hostDao.viewHost(dto.getH_userid());
		PrintWriter out = response.getWriter();
		String num= "";
		// 가입된 아이디가 없으면
		if(hostDao.idCheck(dto.getH_userid()) == 0) {
			out.print("등록되지 않은 아이디입니다.");
			out.close();
		}
		// 가입된 이메일이 아니면
		else if(!dto.getH_email().equals(ck.getH_email())) {
			out.print("등록되지 않은 이메일입니다.");
			out.close();
		}else {
			// 임시 비밀번호 생성
			String pwd = "";
			for(int i = 0; i < 10; i++) {
				pwd+=(char)((Math.random()*26)+97);
			}
			log.info(" 임시 비밀번호  : " + pwd);
			dto.setH_passwd(pwd);
			// 비밀번호 변경 메일 발송
			sendEmail(dto, "findpw", num);
			//패스워드 암호화
			String encodePasswd = pwdEncoder.encode(pwd);
			//암호화한 패스워드를 dto에 저장
			dto.setH_passwd(encodePasswd); 
			// 비밀번호 변경
			hostDao.updatePw(dto);
			out.print("이메일로 임시 비밀번호를 발송하였습니다.");
			out.close();
		}
	}

	@Override
	public int idCheck(String h_userid) {
		int cnt = hostDao.idCheck(h_userid);
		return cnt;
	}
	
	@Override
	public int emailCheck(String h_email) {
		int cnt = hostDao.emailCheck(h_email);
		return cnt;
	}

	@Override
	public void update(HostDTO host) {
		hostDao.update(host);

	}

	@Override
	public void delete(String h_userid,HttpSession session) {
		session.invalidate();
		hostDao.delete(h_userid);

	}
	
	@Override
	public List<RoomsDTO> rooms_Confirm(int start, int end, String h_userid,String select) {
		return hostDao.rooms_Confirm(start, end, h_userid , select);
	}
	
	@Override
	public List<RoomsDTO> rooms_List(int start, int end, String h_userid) {
		return hostDao.rooms_List(start, end, h_userid);
	}
	
	@Override
	public boolean delete_check(RoomsDTO dto) {
		boolean result =hostDao.delete_check(dto);
		return result;
	}
	
	@Override
	public int getRoomCount(String h_userid,String select) {
		return hostDao.getRoomCount(h_userid , select);
	}
	
	@Override
	public int getConfirmRoom(String h_userid) {
		return hostDao.getConfirmRoom(h_userid);
	}

	@Override
	public String findByPasswd(String h_userid) {
		return hostDao.findByPasswd(h_userid);
	}
	
	
	@Override
	public JSONObject getChartData(String h_userid) {
		List<ReservationsDTO> items= hostService.hostMontlyMoney(h_userid);
		//리턴할 최종 json 객체
		JSONObject data=new JSONObject();
		//컬럼을 정의할 json 객체
		JSONObject col1=new JSONObject();
		JSONObject col2=new JSONObject();
		JSONArray title=new JSONArray();
		//json의 cols 객체구성(헤더,제목)
		col1.put("label", "월");
		col1.put("type", "string");
		col2.put("label", "합계");
		col2.put("type", "number");
		//json 배열에 json 객체 추가
		title.add(col1);
		title.add(col2);
		data.put("cols", title);
		//json의 rows 객체구성(바디, 내용)
		JSONArray body=new JSONArray();
		for(ReservationsDTO dto : items) {
			JSONObject name=new JSONObject();//JSONObject는 HashMap과 같음
			name.put("v", dto.getMonth());
			JSONObject money=new JSONObject();
			money.put("v", dto.getMonthlymoney());
			JSONArray row=new JSONArray();
			row.add(name);
			row.add(money);
			JSONObject cell=new JSONObject();
			cell.put("c", row);
			body.add(cell);
		}
		data.put("rows", body);

		return data;
	}
	
	@Override
	public List<ReservationsDTO> hostMontlyMoney(String h_userid) {
		return hostDao.hostMontlyMoney(h_userid);
	}
	
	@Override
	public JSONObject getweeklyData(String h_userid, String today,String week) {
		List<ReservationsDTO> items= hostService.hostWeeklyMoney(h_userid,today,week);
		//리턴할 최종 json 객체
		JSONObject data=new JSONObject();
		//컬럼을 정의할 json 객체
		JSONObject col1=new JSONObject();
		JSONObject col2=new JSONObject();
		JSONArray title=new JSONArray();
		//json의 cols 객체구성(헤더,제목)
		col1.put("label", "일");
		col1.put("type", "string");
		col2.put("label", "합계");
		col2.put("type", "number");
		//json 배열에 json 객체 추가
		title.add(col1);
		title.add(col2);
		data.put("cols", title);
		//json의 rows 객체구성(바디, 내용)
		JSONArray body=new JSONArray();
		for(ReservationsDTO dto : items) {
			JSONObject name=new JSONObject();//JSONObject는 HashMap과 같음
			name.put("v", dto.getDay());
			JSONObject money=new JSONObject();
			money.put("v", dto.getWeeklymoney());
			JSONArray row=new JSONArray();
			row.add(name);
			row.add(money);
			JSONObject cell=new JSONObject();
			cell.put("c", row);
			body.add(cell);
		}
		data.put("rows", body);

		return data;
	}
	
	@Override
	public List<ReservationsDTO> hostWeeklyMoney(String h_userid, String today,String week) {
		return hostDao.hostWeeklyMoney(h_userid,today,week);
	}
	
	@Override
	public JSONObject getRoom_Data(String h_userid) {
		List<ReservationsDTO> items= hostService.getRoomData(h_userid);
		//리턴할 최종 json 객체
		JSONObject data=new JSONObject();
		//컬럼을 정의할 json 객체
		JSONObject col1=new JSONObject();
		JSONObject col2=new JSONObject();
		JSONArray title=new JSONArray();
		//json의 cols 객체구성(헤더,제목)
		col1.put("label", "일");
		col1.put("type", "string");
		col2.put("label", "합계");
		col2.put("type", "number");
		//json 배열에 json 객체 추가
		title.add(col1);
		title.add(col2);
		data.put("cols", title);
		//json의 rows 객체구성(바디, 내용)
		JSONArray body=new JSONArray();
		for(ReservationsDTO dto : items) {
			JSONObject name=new JSONObject();//JSONObject는 HashMap과 같음
			name.put("v", dto.getRoom_name());
			JSONObject money=new JSONObject();
			money.put("v", dto.getYearmoney());
			JSONArray row=new JSONArray();
			row.add(name);
			row.add(money);
			JSONObject cell=new JSONObject();
			cell.put("c", row);
			body.add(cell);
		}
		data.put("rows", body);

		return data;
	}
	
	@Override
	public List<ReservationsDTO> getRoomData(String h_userid) {
		return hostDao.getRoomData(h_userid);
	}
	
	@Override
	public Integer weeklySum(String h_userid, String today,String week) {
		return hostDao.weeklySum(h_userid,today,week);
	}
	
	@Override
	public int yearSum(String h_userid) {
		return hostDao.yearSum(h_userid);
	}
	
	@Override
	public boolean resCheck(String h_userid) {
		return hostDao.resCheck(h_userid);
	}
	
}
