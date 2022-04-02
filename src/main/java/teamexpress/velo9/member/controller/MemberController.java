package teamexpress.velo9.member.controller;

import javax.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import teamexpress.velo9.common.domain.Result;
import teamexpress.velo9.member.dto.FindInfoDTO;
import teamexpress.velo9.member.dto.MailDTO;
import teamexpress.velo9.member.dto.MemberDTO;
import teamexpress.velo9.member.dto.MemberEditDTO;
import teamexpress.velo9.member.dto.MemberNewPwDTO;
import teamexpress.velo9.member.dto.MemberSignupDTO;
import teamexpress.velo9.member.dto.NumberDTO;
import teamexpress.velo9.member.dto.PasswordDTO;
import teamexpress.velo9.member.dto.RandomNumber;
import teamexpress.velo9.member.dto.SocialSignupDTO;
import teamexpress.velo9.member.security.oauth.SessionConst;
import teamexpress.velo9.member.service.MailService;
import teamexpress.velo9.member.service.MemberService;

@RequiredArgsConstructor
@RestController
public class MemberController {

	public static final int INTERVAL = 180;
	private final MemberService memberService;
	private final MailService mailService;

	@GetMapping("/signup")
	public void addMember() {
	}

	@PostMapping("/signup")
	public void addMember(@Validated @RequestBody MemberSignupDTO memberSignupDTO) {
		memberService.join(memberSignupDTO);
	}

	@PostMapping("/sendMail")
	public void sendMail(@Validated @RequestBody MailDTO mailDTO, HttpSession session) {
		String randomNumber = getNumber();
		session.setMaxInactiveInterval(INTERVAL);
		memberService.findEmail(mailDTO);
		mailService.sendMail(mailDTO.getEmail(), randomNumber);
		session.setAttribute(SessionConst.RANDOM_NUMBER, randomNumber);
	}

	@PostMapping("/checkNumber")
	public void checkNumber(NumberDTO numberDTO, HttpSession session) {
		checkInputNumber(numberDTO, session);
	}

	@GetMapping("/setting")
	public MemberDTO editMember(@RequestParam Long memberId) {
		return memberService.getLoginMember(memberId);
	}

	@PostMapping("/setting")
	public MemberDTO editMember(@RequestBody MemberEditDTO memberEditDTO, @RequestParam Long memberId) {
		return memberService.editMember(memberId, memberEditDTO);
	}

	@PostMapping("/changePassword")
	public void changePassword(@RequestBody PasswordDTO passwordDTO, @RequestParam Long memberId) {
		memberService.changePassword(memberId, passwordDTO);
	}

	@PostMapping("/withdraw")
	public void withdrawMember(@RequestBody PasswordDTO passwordDTO, @RequestParam Long memberId) {
		memberService.withdraw(memberId, passwordDTO);
	}

	@PostMapping("/socialSignup")
	public void socialSignup(@Validated @RequestBody SocialSignupDTO socialSignupDTO, @RequestParam Long memberId) {
		memberService.joinSocial(socialSignupDTO, memberId);
	}

	@PostMapping("/findId")
	public void findId(@Validated @RequestBody FindInfoDTO findInfoDTO) {
		String findUsername = memberService.findIdByEmail(findInfoDTO);
		mailService.sendMailFindId(findInfoDTO, findUsername);
	}

	@PostMapping("/findPw")
	public Result findPw(@Validated @RequestBody FindInfoDTO findInfoDTO, HttpSession session) {
		String randomNumber = getNumber();
		Long memberId = memberService.findPw(findInfoDTO);
		mailService.sendMail(findInfoDTO.getEmail(), randomNumber);
		session.setAttribute(SessionConst.RANDOM_NUMBER, randomNumber);
		return new Result(memberId);
	}

	@PostMapping("/changePw")
	public void changePw(@RequestBody MemberNewPwDTO memberNewPwDTO) {
		memberService.changeNewPw(memberNewPwDTO);
	}

	private String getNumber() {
		RandomNumber randomNumber = RandomNumber.get();
		return randomNumber.getNumber();
	}

	private void checkInputNumber(NumberDTO numberDTO, HttpSession session) {
		String certificationNumber = (String) session.getAttribute(SessionConst.RANDOM_NUMBER);

		if (!isEquals(certificationNumber, numberDTO)) {
			throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
		}
		if (isEquals(certificationNumber, numberDTO)) {
			session.removeAttribute(SessionConst.RANDOM_NUMBER);
		}
	}

	private boolean isEquals(String certificationNumber, NumberDTO numberDTO) {
		return certificationNumber.equals(numberDTO.getInputNumber());
	}
}
