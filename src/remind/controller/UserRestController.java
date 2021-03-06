package com.spring.sharepod.controller;


import com.spring.sharepod.dto.request.User.ReIssueRequestDto;
import com.spring.sharepod.dto.request.User.UserLoginRequest;
import com.spring.sharepod.dto.request.User.UserModifyRequestDTO;
import com.spring.sharepod.dto.request.User.UserRegisterRequestDto;
import com.spring.sharepod.dto.response.BasicResponseDTO;
import com.spring.sharepod.dto.response.Board.MyBoardResponseDto;
import com.spring.sharepod.dto.response.Board.RentBuyerResponseDto;
import com.spring.sharepod.dto.response.Board.RentSellerResponseDto;
import com.spring.sharepod.dto.response.Liked.LikedResponseDto;
import com.spring.sharepod.dto.response.User.LoginReturnResponseDTO;
import com.spring.sharepod.dto.response.User.UserInfoResponseDto;
import com.spring.sharepod.entity.User;
import com.spring.sharepod.model.LogOut;
import com.spring.sharepod.model.ReFreshToken;
import com.spring.sharepod.model.Success;
import com.spring.sharepod.model.UserInfo;
import com.spring.sharepod.service.AwsS3Service;
import com.spring.sharepod.service.S3Service;
import com.spring.sharepod.service.UserService;
import com.spring.sharepod.validator.TokenValidator;
import com.spring.sharepod.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
public class UserRestController {
    private final UserService userService;
    private final TokenValidator tokenValidator;
    private final UserValidator userValidator;
    private final AwsS3Service awsS3Service;
    private final S3Service s3Service;
    //private final FileUploadService fileUploadService;

    //????????? ????????????
    @PostMapping("/user/login")
    public LoginReturnResponseDTO loginControll(@RequestBody UserLoginRequest userLoginRequest, HttpServletResponse res){
        return userService.loginReturnDTO(userLoginRequest,res);
    }
    // ?????? ?????????
    @PostMapping("/reissue")
    public ResponseEntity<ReFreshToken> reissue(@RequestBody ReIssueRequestDto reissue,HttpServletResponse res, HttpServletRequest req) {
        // validation check
//        if (errors.hasErrors()) {
//            return response.invalidFields(Helper.refineErrors(errors));
//        }
//        System.out.println(user.getId());
        return userService.reissue(reissue,res,req);
    }

    //?????? ??????
    @PostMapping("/user/logout")
    public ResponseEntity<LogOut> logout(@RequestBody ReIssueRequestDto reIssueRequestDto,HttpServletRequest req) {
        System.out.println(reIssueRequestDto.getAccessToken()+ "reIssueRequestDto.getAccessToken()");
        return userService.logout(reIssueRequestDto,req);
    }


    // ?????? ???????????? (JSON)
    @PostMapping("/user/register")
    public ResponseEntity<Success> createUser(@RequestPart UserRegisterRequestDto userRegisterRequestDto,
                                              @RequestPart MultipartFile imgfile) throws IOException {

        //???????????? ????????? ?????? ?????? validator??? ??????????????? ???????????? ??? ????????????.
        userValidator.validateUserRegisterData(userRegisterRequestDto);

        //?????? ????????? ?????????
        String userimg = s3Service.upload(userRegisterRequestDto, imgfile);

        userRegisterRequestDto.setUserimg(userimg);

        //???????????? ??????
        Long userId = userService.createUser(userRegisterRequestDto);
        return new ResponseEntity<>(new Success("success", "?????? ?????? ?????????????????????."), HttpStatus.OK);
    }

    //?????? ?????? ????????????
    @PatchMapping("/user/{userid}")
    public BasicResponseDTO usermodify(@PathVariable Long userid,
                                       @RequestPart UserModifyRequestDTO userModifyRequestDTO,
                                       @RequestPart MultipartFile userimgfile, @AuthenticationPrincipal User user) throws IOException {
        //????????? userid ?????? ??????
        tokenValidator.userIdCompareToken(userid, user.getId());
        System.out.println("modified userid =====  "  + user.getId());

        //?????? request vaildator ??????
        userValidator.validateUserChange(userModifyRequestDTO);

        //???????????? ????????? ???????????????
        if(!Objects.equals(userimgfile.getOriginalFilename(), "")){
            //????????? ?????? ?????? ??? ?????? ?????? ?????? ??? requestDto??? setUserimg ??????
            userModifyRequestDTO.setUserimg(awsS3Service.ModifiedProfileImg(user.getUserimg().substring(user.getUserimg().lastIndexOf("/")+1), user.getNickname(), userimgfile));
        }else {
            userModifyRequestDTO.setUserimg(user.getUserimg());
        }

        return userService.usermodifyService(userid, userModifyRequestDTO);
    }

    //?????? ????????????
    @DeleteMapping("/user/{userid}")
    public ResponseEntity<Success> DeleteUser(@PathVariable Long userid, @RequestBody UserLoginRequest userLoginRequest, @AuthenticationPrincipal User user){
        //????????? userid ?????? ??????
        tokenValidator.userIdCompareToken(userid,user.getId());

        String nickname = userService.UserDelete(userid, userLoginRequest);
        return new ResponseEntity<>(new Success("success", nickname + " ?????? ???????????? ??????????????????."),HttpStatus.OK);
    }

    //??????????????? ????????????
    @GetMapping("/user/{userid}")
    public ResponseEntity<UserInfo> getBoardList(@PathVariable Long userid, @AuthenticationPrincipal User user) {

        //????????? userid ?????? ??????
        tokenValidator.userIdCompareToken(userid,user.getId());

        //????????? ????????? ????????????
        UserInfoResponseDto userinfo = userService.getUserInfo(userid);
        List<LikedResponseDto> userlikeboard = userService.getUserLikeBoard(userid);
        List<MyBoardResponseDto> usermyboard = userService.getMyBoard(userid);
        List<RentBuyerResponseDto> rentbuyer = userService.getBuyList(userid);
        List<RentSellerResponseDto> rentseller = userService.getSellList(userid);
        return new ResponseEntity<>(new UserInfo("success", "??? ?????? ???????????? ??????", userinfo,userlikeboard,usermyboard,rentbuyer,rentseller), HttpStatus.OK);
    }
}