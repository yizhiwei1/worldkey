package com.worldkey.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.worldkey.entity.Comment;
import com.worldkey.entity.CommentApp;
import com.worldkey.entity.InformationAll;
import com.worldkey.entity.Praise;
import com.worldkey.entity.PraiseApp;
import com.worldkey.entity.Users;
import com.worldkey.mapper.CommentMapper;
import com.worldkey.mapper.HistoryMapper;
import com.worldkey.mapper.InformationAllMapper;
import com.worldkey.mapper.PraiseCommentMapper;
import com.worldkey.mapper.PraiseCommentNumMapper;
import com.worldkey.mapper.UsersMapper;
import com.worldkey.service.CommentService;
import com.worldkey.service.UsersService;
import com.worldkey.worldfilter.WordFilter;


import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author HP
 */
@Service
public class CommentServiceImpl implements CommentService {
    @Resource
    private WordFilter wordFilter;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private UsersService usersService;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
	private UsersMapper uMapper;
    @Resource
	private InformationAllMapper iMapper;
    @Resource
	private PraiseCommentMapper pMapper;
    @Resource
	private HistoryMapper hMapper;
    @Resource
	private PraiseCommentNumMapper pnMapper;

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public int deleteByPrimaryKey(Long commentId, Integer information) {
        Set<String> keys = redisTemplate.keys("comment" + information + "-*");
        redisTemplate.delete(keys);
        return this.commentMapper.deleteByPrimaryKey(commentId);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Integer insert(Comment record) throws UnsupportedEncodingException {
        Set<String> keys = redisTemplate.keys("comment" + record.getInformation() + "-*");
        redisTemplate.delete(keys);
        return this.commentMapper.insert(record);
    }
    

    @Override
    public PageInfo<Comment> selectByInformationOrderByIdDesc(Long information, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize, true);
        List<Comment> comments = this.commentMapper.selectByInformationOrderByIdDesc(information);
        return new PageInfo<>(comments);
    }
    @Override
    public PageInfo<Comment> selectByInformationOrderByIdDesc1(Long information, Integer pageNum, Integer pageSize,String token) {
        PageHelper.startPage(pageNum, pageSize, true);
        List<Comment> comments = this.commentMapper.selectByInformationOrderByIdDesc1(information);
        PageInfo<Comment> pageInfo = new PageInfo<>(comments);
        for(Comment c:comments){
        	Date s = this.commentMapper.s();
        	SimpleDateFormat dfss = new SimpleDateFormat("yy年MM月dd日 HH:mm"); 
            String datesss = dfss.format(s);
        	String ss = datesss.substring(0,3);
        	
        	Date days = c.getGmtCreate();
        	SimpleDateFormat dfs = new SimpleDateFormat("yy年MM月dd日 HH:mm"); 
            String datess = dfs.format(days);
        	String sss = datess.substring(0,3);
        	
        	if(ss.equals(sss)){
        		c.setJudge(1);
        	}
        	List<Comment> comments1 = this.commentMapper.selectByCommentsPraise(c.getCommentId());
        	c.setList(comments1);
        	c.setSize(comments.indexOf(c));
        	if(c.getPraiseNum()==null){
        		c.setPraiseNum(0);
        	}
        	c.setPages(pageInfo.getPages());
        	//判断点赞 0为点赞  1点赞
    		if(token!=null){
    			Users user = usersService.findByToken(token);
    			Integer i = this.commentMapper.status(user.getId(), c.getCommentId());
    			c.setStatus(i);
    		}
        }  
        return pageInfo;
    }
    @Override
    public PageInfo<Comment> selectByInformationOrderByIdDesc2(Long information, Integer pageNum, Integer pageSize,String token) {
        PageHelper.startPage(pageNum, pageSize, true);
        List<Comment> comments = this.commentMapper.selectByInformationOrderByIdDesc2(information);
        PageInfo<Comment> pageInfo = new PageInfo<>(comments);
        for(Comment c:comments){
        	Date s = this.commentMapper.s();
        	SimpleDateFormat dfss = new SimpleDateFormat("yy年MM月dd日 HH:mm"); 
            String datesss = dfss.format(s);
        	String ss = datesss.substring(0,3);
        	
        	Date days = c.getGmtCreate();
        	SimpleDateFormat dfs = new SimpleDateFormat("yy年MM月dd日 HH:mm"); 
            String datess = dfs.format(days);
        	String sss = datess.substring(0,3);
        	
        	if(ss.equals(sss)){
        		c.setJudge(1);
        	}
        	List<Comment> comments1 = this.commentMapper.selectByCommentsPraise(c.getCommentId());
        	c.setList(comments1);
        	c.setSize(comments.indexOf(c));
        	if(c.getPraiseNum()==null){
        		c.setPraiseNum(0);
        	}
        	c.setPages(pageInfo.getPages());
        	//判断点赞 0为点赞  1点赞
    		if(token!=null){
    			Users user = usersService.findByToken(token);
    			Integer i = this.commentMapper.status(user.getId(), c.getCommentId());
    			c.setStatus(i);
    		}
        }   
        return pageInfo;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public int addReply(Comment comment, String token) throws Exception {
        //敏感词过滤
        String infoStr = comment.getInfo();
        boolean contains = wordFilter.isContains(infoStr);
        if (contains) {
            throw new Exception("存在非法词语,请文明用语");
        }
        //登录验证
        Users user = usersService.findByToken(token);
        //未登录
        if (token == null || user == null) {
            throw new Exception("请先登陆");
        }
        Comment comment1 = this.selectByPrimaryKey(comment.getComment());
        if (comment1 == null) {
            throw new Exception("记录不存在");
        }
        comment.setGmtCreate(new Date(System.currentTimeMillis()));
        comment.setUsers(user);
        //设置为评论的回复
        comment.setType(Byte.valueOf("1"));
        this.commentMapper.addReplyCount(comment.getComment());
        Set<String> keys = redisTemplate.keys("comment" + comment1.getInformation()+ "-*");
        keys.addAll(redisTemplate.keys("getReplyComment" + comment.getComment() + ",*"));
        redisTemplate.delete(keys);
        return this.insert(comment);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public int addReply1(Comment comment, String token) throws Exception {
        //敏感词过滤
        String infoStr = comment.getInfo();
        boolean contains = wordFilter.isContains(infoStr);
        if (contains) {
            throw new Exception("存在非法词语,请文明用语");
        }
        //登录验证
        Users user = usersService.findByToken(token);
        //未登录
        if (token == null || user == null) {
            throw new Exception("请先登陆");
        }
        Comment comment1 = this.selectByPrimaryKey(comment.getComment());
        if (comment1 == null) {
            throw new Exception("记录不存在");
        }
        comment.setGmtCreate(new Date(System.currentTimeMillis()));
        comment.setUsers(user);
        //设置为评论的回复
        comment.setType(Byte.valueOf("2"));
        Long a = this.commentMapper.comment(comment.getComment());
        this.commentMapper.addReplyCount(a);
        this.commentMapper.addReplyCount(comment.getComment());
        Set<String> keys = redisTemplate.keys("comment" + comment1.getInformation()+ "-*");
        keys.addAll(redisTemplate.keys("getReplyComment" + comment.getComment() + ",*"));
        redisTemplate.delete(keys);
        return this.insert(comment);
    }
    
    

    @Override
    public PageInfo<Comment> getReply(Long comment, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Comment>list=this.commentMapper.selectByComment(comment);
        return new PageInfo<>(list);
    }
    
    @Override
    public PageInfo<Comment> getReply1(Long comment, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Comment>list=this.commentMapper.selectByComment1(comment);
        return new PageInfo<>(list);
    }
    
    @Override
    public PageInfo<Comment> getReplyTime(Long comment, Integer pageNum, Integer pageSize,String token) {
        PageHelper.startPage(pageNum,pageSize);
        List<Comment>list=this.commentMapper.getReplyTime(comment);
        PageInfo<Comment> pageInfo = new PageInfo<>(list);
        for(Comment c:list){
        	Date s = this.commentMapper.s();
        	SimpleDateFormat dfss = new SimpleDateFormat("yy年MM月dd日 HH:mm"); 
            String datesss = dfss.format(s);
        	String ss = datesss.substring(0,3);
        	
        	Date days = c.getGmtCreate();
        	SimpleDateFormat dfs = new SimpleDateFormat("yy年MM月dd日 HH:mm"); 
            String datess = dfs.format(days);
        	String sss = datess.substring(0,3);
        	
        	if(ss.equals(sss)){
        		c.setJudge(1);
        	}
        	if(c.getPraiseNum()==null){
        		c.setPraiseNum(0);       		
        	}
        	c.setSize(list.indexOf(c));
        	c.setPages(pageInfo.getPages());
        	Comment cc = this.commentMapper.selectByPrimaryKey(comment);
        		c.setToUserId(cc.getUsers().getId());
        		//判断点赞 0为点赞  1点赞
        		if(token!=null){
        			Users user = usersService.findByToken(token);
        			Integer i = this.commentMapper.status(user.getId(), c.getCommentId());
        			c.setStatus(i);
        		}
        }
        return pageInfo;
    }
    
    @Override
    public PageInfo<Comment> getReplyPraise(Long comment, Integer pageNum, Integer pageSize,String token) {
        PageHelper.startPage(pageNum,pageSize);
        List<Comment>list=this.commentMapper.getReplyPraise(comment);
        PageInfo<Comment> pageInfo = new PageInfo<>(list);
        for(Comment c:list){
        	Date s = this.commentMapper.s();
        	SimpleDateFormat dfss = new SimpleDateFormat("yy年MM月dd日 HH:mm"); 
            String datesss = dfss.format(s);
        	String ss = datesss.substring(0,3);
        	
        	Date days = c.getGmtCreate();
        	SimpleDateFormat dfs = new SimpleDateFormat("yy年MM月dd日 HH:mm"); 
            String datess = dfs.format(days);
        	String sss = datess.substring(0,3);
        	
        	if(ss.equals(sss)){
        		c.setJudge(1);
        	}
        	
        	if(c.getPraiseNum()==null){
        		c.setPraiseNum(0);
        	}
        	c.setSize(list.indexOf(c));
        	c.setPages(pageInfo.getPages());
    		Comment cc = this.commentMapper.selectByPrimaryKey(comment);
    		c.setToUserId(cc.getUsers().getId());
    		//判断点赞 0为点赞  1点赞
    		if(token!=null){
    			Users user = usersService.findByToken(token);
    			Integer i = this.commentMapper.status(user.getId(), c.getCommentId());
    			c.setStatus(i);
    		}
        }
        return pageInfo;
    }

    @Override
    public Comment selectByPrimaryKey(Long commentId) {
        return this.commentMapper.selectByPrimaryKey(commentId);
    }

	@Override
	public List<CommentApp> comment(Long userId) {
		List<Long> chooseUsers = this.commentMapper.chooseUsers(userId);
		List<Comment> comment = this.commentMapper.Comment(userId);
		List<CommentApp> commentApp1 = new ArrayList<CommentApp>();
		List<CommentApp> commentApp2 = new ArrayList<CommentApp>();
		List<CommentApp> commentApp = new ArrayList<CommentApp>();
		for (int i = 0; i < comment.size(); i++) {
			CommentApp c = new CommentApp();
			c.setInfo(comment.get(i).getInfo());
			c.setInformation(comment.get(i).getInformation());
			c.setGmtCreate(comment.get(i).getGmtCreate());
			Long id = comment.get(i).getInformation();
			InformationAll a = this.iMapper.selectinfo(id);
			c.setTitle(a.getTitle());
			c.setTitleImg(a.getTitleImg());
			c.setWebUrl(a.getWeburl());
			commentApp1.add(c);
		}
		for (int i = 0; i < chooseUsers.size(); i++) {
			CommentApp c = new CommentApp();
			c.setUserId(chooseUsers.get(i));
			Long id = chooseUsers.get(i);
			Users u = this.uMapper.selectPetNameById(id);
			c.setPetName(u.getPetName());
			c.setLoginName(u.getLoginName());
			c.setHeadImg(u.getHeadImg());
			commentApp2.add(c);
		}
		for (int i = 0; i < commentApp1.size(); i++) {
			CommentApp c1 = commentApp1.get(i);
			CommentApp c2 = commentApp2.get(i);
			CommentApp c = new CommentApp();
			c.setUserId(c2.getUserId());
			c.setPetName(c2.getPetName());
			c.setLoginName(c2.getLoginName());
			c.setHeadImg(c2.getHeadImg());
			c.setInfo(c1.getInfo());
			c.setInformation(c1.getInformation());
			c.setTitle(c1.getTitle());
			c.setTitleImg(c1.getTitleImg());
			c.setWebUrl(c1.getWebUrl());
			c.setGmtCreate(c1.getGmtCreate());
			commentApp.add(c);
		}
		return commentApp;
	}

	@Override
	public int deleteComment(Long commentId) {
		List<Long> a = this.commentMapper.selectCommentId(commentId);
		if(a!=null){
			for(Long b:a){
			this.commentMapper.delete2(b);
			this.pMapper.deleteC(b);
			this.pnMapper.deleteComment(Integer.parseInt(b+""));
		}
		}
		this.pMapper.deleteC(commentId);
		this.pnMapper.deleteComment(Integer.parseInt(commentId+""));
		int c = this.commentMapper.delete(commentId);
		return c;
	}
	
	@Override
	public int deleteCommentHistory(Long id){
		int c = this.hMapper.deleteInformation(id);
		return c;
	}


}
