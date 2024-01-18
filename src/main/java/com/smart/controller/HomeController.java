package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smart.config.AuthenticationService;
import com.smart.config.UserDetailServiceImple;
import com.smart.dao.UserRepository;
import com.smart.entity.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeController {

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuthenticationService authService;

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("title", "Home-smart contact manager");
		return "home";
	}

	@GetMapping("/about")
	public String about(Model model) {
		model.addAttribute("title", "about-smart contact manager");
		return "about";
	}

	@GetMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title", "Register-smart contact manager");
		model.addAttribute("user", new User());
		return "signup";
	}

	
	private String generateUniqueFileName() {
		String timeStamp = String.valueOf(System.currentTimeMillis());
		String randomString = UUID.randomUUID().toString().replaceAll("-", "");

		return "image_" + timeStamp + "_" + randomString;
	}
//	handler for user registration

	@PostMapping("/register")
	public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result,
			@RequestParam(value = "agreement", defaultValue = "false") boolean agreement, Model model,
			HttpSession session,@RequestParam("profileImage") MultipartFile file, RedirectAttributes redirectAttributes) {

		try {

			if (!agreement) {
				System.out.println("you have not agreed the terms and conditions");
				throw new Exception("you have not agreed the terms and conditions");
			}

			if (result.hasErrors()) {
				model.addAttribute("user", user);
				return "signup";
			}

			user.setRole("User_Role");
			user.setStatus(true);
			//user.setImage("default.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			
			
//			making changes here
			
			
			 if (!file.isEmpty()) {
		            // Process and save the profile image
		            String originalFileName = file.getOriginalFilename();
		            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
		            String uniqueFileName = generateUniqueFileName() + fileExtension;

		            // Save the file with the unique name
		            user.setImage(uniqueFileName);
		            File saveFile = new ClassPathResource("static/image").getFile();
		            Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + uniqueFileName);

		            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
		            System.out.println("Profile image added with a unique name: " + uniqueFileName);
		        } else {
		            // Set a default image if no file is provided
		            user.setImage("default.png");
		        }
			
			
			
//          end here
		
			userRepository.save(user);

			model.addAttribute("user", new User()); // if everything fine then new user will be sent
			redirectAttributes.addFlashAttribute("sessionMessage",
					new Message("Successfully Registered", "alert-success"));
			return "redirect:/signup"; // Redirect to the sign up page

		} catch (Exception e) {

			// model.addAttribute("user", user); // if there is an error then we send the
			// details of the user
			redirectAttributes.addFlashAttribute("sessionMessage",
					new Message("Something went wrong " + e.getMessage(), "alert-danger"));
			redirectAttributes.addFlashAttribute("user", user); // if there is an error then we send the details of the
																// user
			System.out.println("entered catch block");
			System.out.println(user);
			return "redirect:/signup"; // Redirect to the sign up page

		}

	}

	@GetMapping("/signin")
	public String showSigninPage(Model model) {
		model.addAttribute("title", "Login-smart contact manager");

	    // Logic to render your signup page
	    return "signin"; // Return the name of the Thymeleaf template for the signup page
	}

	
	
	@PostMapping("/signin")
	public String customLogin(@RequestParam("username") String username, @RequestParam("password") String password,
			HttpSession session, Model model) {

		model.addAttribute("title", "Login");

		// Your authentication logic
		if (authService.authenticate(username, password)) {
			// Authentication successful, redirect to dashboard
			return "redirect:/user/dashboard";
		} else {
			// Authentication failed, redirect to login with error
			return "redirect:/signin?error";
		}

//		return "signin";
	}
	
	@GetMapping("/logout")
	public String logout()
	{
		return "redirect:/signin?logout";
	}

}
