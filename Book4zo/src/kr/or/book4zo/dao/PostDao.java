package kr.or.book4zo.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import kr.or.book4zo.dto.PostDto;

public class PostDao {

	static DataSource ds;
	// 1. 생성자에서 ds 객체 초기화
	// 2. static 초기자 : static{ static 멤버 초기화 }
	Connection conn = null;
	PreparedStatement pstmt = null;
	ResultSet rs = null;

	static {
		InitialContext ctx;
		try {
			ctx = new InitialContext();
			Context envctx = (Context) ctx.lookup("java:comp/env"); // 기본설정
			ds = (DataSource) envctx.lookup("/jdbc/oracle");// context.xml 에서 name="jdbc/oracle"
		} catch (Exception e) {
			System.out.println("look up Fail : " + e.getMessage());
		}
		// Context context = new InitialContext(); //이름기반 검색
		// ds = (DataSource)context.lookup("java:comp/env/jdbc/oracle"); ///jdbc/oracle
		// pool 검색

	}

	///////////////////////////////////////////////////////////////////////////////// DB연결부분
	// 연결 해제
	public void closed() {
		try {
			if (rs != null)
				rs.close();
			if (pstmt != null)
				pstmt.close();
			if (conn != null)
				conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 글의 개수를 구하는 함수
	// List.jsp
	public int getListCount() {
		// select count(*) from post
		int rowcount = 0;
		try {
			conn = ds.getConnection();
			pstmt = conn.prepareStatement("select count(*) from post");
			rs = pstmt.executeQuery();
			if (rs.next()) {
				rowcount = rs.getInt(1);
			}
		} catch (Exception e) {

		} finally {
			closed();
		}
		return rowcount;
	}

	// Post의 리스트 구성
	// List.jsp
	public List getpostList(int page, int limit) {
		// 글 목록 보기
		/*
		 * 18건 pagesize = 5 pagelist = 4개 1~5 , 5~10 , 11~15 , 16~18 pageindex = 3
		 * 11~15개의 데이터
		 * 
		 * BoardDto page = new BoardDto(); =>Table Row 한건 여러건의 데이터 배열
		 */
		// List.jsp
		// page =2 , limit =10(pagesize)

		String post_list_sql = "select * from " + "(select rownum rnum,post_seq,user_id,book_seq,"
				+ "post_title,post_contents,post_date," + "post_views,post_upload_file from" + "(select * from post )) "
				+ "where rnum>=? and rnum<=?";

		List list = new ArrayList();
		// List<Article> articleList = new ArrayList<Article>();
		// 권장방식 generic 사용
		int startrow = (page - 1) * 10 + 1;
		// Code //읽기 시작할 row 번호.
		// (2-1)*10+1 (1)*10+1 10 +1 11
		// page 없어 : 1
		int endrow = startrow + limit - 1; // 읽을 마지막 row 번호.
		// 11 +10 => 21-1 => 20
		// page 없어 : 10
		try {
			conn = ds.getConnection();
			pstmt = conn.prepareStatement(post_list_sql);
			pstmt.setInt(1, startrow); // 11 21 code
			pstmt.setInt(2, endrow); // 20 30 code
			rs = pstmt.executeQuery();

			while (rs.next()) {
				PostDto postdto = new PostDto(); // 한 건의
				postdto.setPost_seq(rs.getInt("post_seq"));
				postdto.setUser_id(rs.getString("user_id"));
				postdto.setBook_seq(rs.getInt("book_seq"));
				postdto.setPost_title(rs.getString("post_title"));
				postdto.setPost_contents(rs.getString("post_contents"));
				postdto.setPost_date(rs.getString("post_date"));
				postdto.setPost_views(rs.getInt("post_views"));
				postdto.setPost_upload_file(rs.getString("post_upload_file"));

				list.add(postdto); // key point (여러건의 데이터 collection사용)

			}
			System.out.println("list도 궁금하다: " + list);
			return list;
		} catch (Exception ex) {
			System.out.println("getPostList 에러 : " + ex); // 에러없다.
		} finally {
			closed();
		}
		return null;
	}

	// 글 내용 보기.
	public PostDto getDetail(int num) throws Exception {
		PostDto postdto = null;
		try {
			conn = ds.getConnection();
			pstmt = conn.prepareStatement("select * from post where post_seq = ?");

			pstmt.setInt(1, num);

			rs = pstmt.executeQuery();

			if (rs.next()) {
				postdto = new PostDto();
				postdto.setPost_seq(rs.getInt("post_seq"));
				postdto.setUser_id(rs.getString("user_id"));
				postdto.setBook_seq(rs.getInt("book_seq"));
				postdto.setPost_title(rs.getString("post_title"));
				postdto.setPost_contents(rs.getString("post_contents"));
				postdto.setPost_date(rs.getString("post_date"));
				postdto.setPost_views(rs.getInt("post_views"));
				postdto.setPost_upload_file(rs.getString("post_upload_file"));
			}
			return postdto;
		} catch (Exception ex) {
			System.out.println("getDetail 에러 : " + ex);
		} finally {
			if(rs!=null)try{rs.close();}catch(SQLException ex){}
			if(pstmt !=null)try{pstmt.close();}catch(SQLException ex){}
			if(conn !=null)try{conn.close();}catch(SQLException ex){}
		}
			return null;
	}

	// 글 등록
	// write.jsp
	public int postInsert(PostDto postdto) {
		
		System.out.println("postdto 확인 : "+postdto);
		
		int num = 0;
		String sql = "";
		String sql2 = "";

		int postNum = 0;

		try {
			conn = ds.getConnection();
//			pstmt = conn.prepareStatement("select max(Post_Seq) from post");
//			rs = pstmt.executeQuery();
//
//			if (rs.next())
//				num = rs.getInt(1) + 1;
//			else
//				num = 1;
			
			if(postdto.getPost_upload_file() == null){
			    postdto.setPost_upload_file("N");
			   }
			
			sql = "insert into post (POST_SEQ,USER_ID,BOOK_SEQ,POST_TITLE,";
			sql += "POST_CONTENTS,POST_DATE,POST_VIEWS,POST_UPLOAD_FILE)";
			sql	+= "values(post_seq.NEXTVAL, ?, ?, ?, ?, sysdate, ?, ? )";


			pstmt = conn.prepareStatement(sql);
			//pstmt.setInt(1, num);
			pstmt.setString(1, "a"); // postdto.getUser_id()
			pstmt.setInt(2, 1); //postdto.getBook_Seq()
			pstmt.setString(3, postdto.getPost_title());
			pstmt.setString(4, postdto.getPost_contents());
			//pstmt.setString(5, postdto.getPost_date());
			pstmt.setInt(5, postdto.getPost_views());
			pstmt.setString(6, postdto.getPost_upload_file());
		
				

			int queryResult = pstmt.executeUpdate();
			
			System.out.println("insert 결과 : "+queryResult);
			
			//포스트번호에 최대값을 만들어주고  그렇게해서 셋리쿼스트해서 꺼내온다
			//sql2 = "select max(POST_SEQ) from post";
			//pstmt = conn.prepareStatement(sql2);
		
			//postNum = pstmt.ex
			
			sql2 = "SELECT MAX(POST_SEQ) FROM POST";
			pstmt = conn.prepareStatement(sql2);
			rs = pstmt.executeQuery();
			
			while(rs.next()) {
				postNum = rs.getInt(1);
			}
			
			
			System.out.println("result 값을 :"+queryResult);

			System.out.println("currentPostSeq 확인 : " + postNum);
								

			if (queryResult == 0) {
				System.out.println("포스트 작성 실패");
				return 0;
			}							
			return postNum;
			
		} catch (Exception ex) {
			System.out.println("postInsert 에러 : " + ex);
		} finally {
			closed();
		}
		return postNum;
	}

	public boolean PostModify(PostDto modifyPost) throws Exception {

		String sql = "update post set post_contents=?,post_title=? where post_seq=?";

		try {
			conn = ds.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, modifyPost.getPost_contents());
			pstmt.setString(2, modifyPost.getPost_title());
			pstmt.setInt(3, modifyPost.getPost_seq());
			pstmt.executeUpdate();
			return true;
		} catch (Exception ex) {
			System.out.println("postModify 에러 : " + ex);
		} finally {
			if(rs!=null)try{rs.close();}catch(SQLException ex){}
			if(pstmt!=null)try{pstmt.close();}catch(SQLException ex){}
			if(conn!=null)try{conn.close();}catch(SQLException ex){}
			}
		return false;
	}

	// 글 삭제
	// delete.jsp
	public boolean PostDelete(int num) {

		String Post_delete_sql = "delete max(post_seq) from post where post_seq=?";

		int result = 0;

		try {
			conn = ds.getConnection();
			pstmt = conn.prepareStatement(Post_delete_sql);
			pstmt.setInt(1, num);
			result = pstmt.executeUpdate();
			if (result == 0)
				return false;

			return true;
		} catch (Exception ex) {
			System.out.println("postDelete 에러 : " + ex);
		} finally {
			closed();
		}

		return false;
	}

	/*
	 * //음 이거 대기 // 조회수 업데이트 // view.jsp public void setReadCountUpdate(int num)
	 * throws Exception {
	 * 
	 * String sql = "update post set BOARD_READCOUNT = " +
	 * "BOARD_READCOUNT+1 where BOARD_NUM = " + num;
	 * 
	 * try { conn = ds.getConnection(); pstmt = conn.prepareStatement(sql);
	 * pstmt.executeUpdate(); } catch (SQLException ex) {
	 * System.out.println("setReadCountUpdate 에러 : " + ex); } finally { closed(); }
	 * }
	 */
	// 글쓴이인지 확인
	// 인증형 게시판 (로그인) (글쓴이 = 로그인한 사용자)
	// 비인증형 게시판(일반 사용자) : 글을 수정 , 삭제 => 비번
	// 삭제 수정하려는 글에 확인작업 (비번)
	// Modify.jsp , Delete.jsp
	
	public boolean isPostWriter(int num, String user_id) {
		
		
		System.out.println("user_id : " + user_id);
		
		
		String post_sql = "select max(post_seq) from post where user_id=?";
		try {
			conn = ds.getConnection();
			pstmt = conn.prepareStatement(post_sql);
			pstmt.setInt(1, num);
			rs = pstmt.executeQuery();
			rs.next();

			if (user_id.equals(rs.getString("user_id"))) {
				return true;
			}
		} catch (SQLException ex) {
			System.out.println("ispostWriter 에러 : " + ex);
		} finally {
			closed();
		}
		return false;
	}

	/*
	 * //하나의 게시글을 리넡하는 메소드 public PostDto getOnePost(int num) throws Exception{
	 * //리턴타입 선언
	 * 
	 * PostDto postdto = null();
	 * 
	 * try{
	 * 
	 * String sql ="select max(Post_Seq) from post"; //쿼리실행객체
	 * pstmt=conn.prepareStatement(sql); pstmt.setInt(1, num); //쿼리 실행후 결과를 리턴
	 * rs=pstmt.executeQuery(); if(rs.next()){ postdto = new PostDto();
	 * postdto.setPost_seq(rs.getInt("post_seq"));
	 * postdto.setUser_id(rs.getString("user_id"));
	 * postdto.setBook_seq(rs.getInt("book_seq"));
	 * postdto.setPost_title(rs.getString("post_title"));
	 * postdto.setPost_contents(rs.getString("post_contents"));
	 * postdto.setPost_date(rs.getString("post_date"));
	 * postdto.setPost_views(rs.getInt("post_views"));
	 * postdto.setPost_upload_file(rs.getString("post_upload_file")); } return
	 * postdto; }catch(Exception e){ e.printStackTrace(); }finally{ //자원 반납
	 * closed(); } return null; }
	 */

	//////////////////////
	// 글등록 테스트
	// 글 등록
	// write.jsp

	//하나의 게시글을 리넡하는 메소드
	public PostDto getOnePost(int num){
		//리턴타입 선언
		PostDto postdto =new PostDto();
		int postNum = 0;
		
		try{
			//조회수가 증가해야할까??
			//조회수 증가쿼리
		//	String readsql ="update post set readcount= readcount+1 where num=?";
		//	pstmt =conn.prepareStatement(readsql);
		//	pstmt.setInt(1, num);
		//	pstmt.executeUpdate();
			
			
			//쿼리준비
			String sql ="select max(Post_Seq) from post";
			//쿼리실행객체
			pstmt=conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				postNum = rs.getInt(1);
			}
		
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			//자원 반납
			closed();
		}
		return postdto;
	}
	/*
	 * public PostDto getOnePost(String user_id) throws Exception{ PostDto postdto =
	 * null; try{ conn = ds.getConnection(); pstmt = conn.prepareStatement(
	 * "select max(Post_Seq) from post where user_id=? "); // where Post_Seq = ?
	 * pstmt.setString(1, user_id);
	 * 
	 * rs= pstmt.executeQuery();
	 * 
	 * if(rs.next()){
	 * 
	 * postdto = new PostDto(); postdto.setPost_seq(rs.getInt("post_seq"));
	 * postdto.setUser_id(rs.getString("user_id"));
	 * postdto.setBook_seq(rs.getInt("book_seq"));
	 * postdto.setPost_title(rs.getString("post_title"));
	 * postdto.setPost_contents(rs.getString("post_contents"));
	 * postdto.setPost_date(rs.getString("post_date"));
	 * postdto.setPost_views(rs.getInt("post_views"));
	 * postdto.setPost_upload_file(rs.getString("post_upload_file")); } return
	 * postdto; }catch(Exception ex){ System.out.println("getDetail 에러 : " + ex);
	 * }finally{ closed(); } return null; }
	 */
}
