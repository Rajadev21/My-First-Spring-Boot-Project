package com.smart.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smart.dao.UserRepository;
import com.smart.entity.User;
import com.smart.helper.Message;
import com.smart.service.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ForgotController {

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepository userRepository;
	
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	Random random = new Random();

	@GetMapping("/forgotPassword")
	public String forgotPasswordHandler(Model model) {
		
		model.addAttribute("title","forgot-Password");
		return "forgotPage";
	}

	@PostMapping("/send-otp")
	public String handlingOtp(@RequestParam("email") String email, HttpSession session,RedirectAttributes redirectAttributes,Model model) {
		System.out.println("your email " + email);

//		generating 4 digit otp

		//int otp = random.nextInt(999999);
		// generating 6-digit OTP
	    int otp = 100_000 + random.nextInt(900_000);

		System.out.println("your otp is" + otp);

//		code for send otp to email

		String subject = "OTP from Smart Contact Manager";

		String message = "<h1> OTP is " + otp + "</h1>";
		String to = email;

		boolean flag = emailService.sendEmail(subject, message, to);

		if (flag == true) {
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			session.setAttribute("message", "We have sent OTP Successfully to your email");

			redirectAttributes.addFlashAttribute("sessionMessage",
					new Message("We have sent OTP Successfully to your email", "alert-success"));
			model.addAttribute("title","verify-otp");

			
			return "varifyOtp";

		} else {

			return "forgotPage";
		}

	}

	

	@PostMapping("/verify-otp")

	public String verifyotp(@RequestParam("otp") int otp, HttpSession session, RedirectAttributes redirectAttributes,Model model)

	{
		
		
		int myotp = (int) session.getAttribute("myotp");

		System.out.println("User otp which entered from form : " + otp);
		System.out.println("myotp which is generated ny java : " + myotp);

		String email = (String) session.getAttribute("email");

		if (myotp == otp) {
//			password change form

			User user = userRepository.getuserByUserName(email);

			if (user == null) {
				// send error message
				session.setAttribute("message", "User does not exist with this email try again");
				redirectAttributes.addFlashAttribute("sessionMessage",
						new Message("User does not exist with this email try again", "alert-danger"));
				return "redirect:/send-otp";
			} else {

				// send change password form
				
				model.addAttribute("title","Change-password");


				return "passwordChangeForm";
			}

		} else {

			session.setAttribute("message", "You have entered wrong otp");
			redirectAttributes.addFlashAttribute("sessionMessage",
					new Message("You have entered wrong otp", "alert-danger"));

			return "redirect:/verify-otp";
		}

	}
	@GetMapping("/verify-otp")
	public String showVerifyOtpPage(HttpSession session, RedirectAttributes redirectAttributes,Model model) {
		//model.addAttribute("title","verify-otp");
		session.setAttribute("message", "You have entered wrong otp");

		redirectAttributes.addFlashAttribute("sessionMessage",
				new Message("You have entered wrong otp", "alert-danger"));
		return "varifyOtp";
	}
	
	
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword,HttpSession session,Model model)
	{
		String email = (String) session.getAttribute("email");
		User user = userRepository.getuserByUserName(email);
		
		user.setPassword(bCryptPasswordEncoder.encode(newpassword));
		
		userRepository.save(user);

		
		return "redirect:/signin?change=password changed successfully";
	}
	
	
	
	
	
}
