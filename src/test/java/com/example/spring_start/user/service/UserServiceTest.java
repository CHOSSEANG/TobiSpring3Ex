package com.example.spring_start.user.service;

import com.example.spring_start.mail.MailSender;
import com.example.spring_start.user.dao.UserDao;
import com.example.spring_start.user.domain.Level;
import com.example.spring_start.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.spring_start.user.service.UserServiceImpl.MIN_LOGCOUNT_FOR_SILVER;
import static com.example.spring_start.user.service.UserServiceImpl.MIN_RECOMMEND_FOR_GOLD;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "/test-applicationContext.xml")
public class UserServiceTest {
    //@formatter:off
    @Autowired UserService userService;
    List<User> users; // 테스트 픽스처
    @Autowired private UserDao userDao;
    @Autowired MailSender mailSender;
    UserServiceImpl userServiceImpl;
    @Autowired ApplicationContext context;
    TestUserService testUserService;
    //@formatter:on

    @BeforeEach
    public void setUp() {
        users = Arrays.asList(
                new User("bumjin", "박범진", "p1", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER - 1, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER + 1, 0),
                new User("erwins", "신승한", "p3", Level.SILVER, 60, MIN_RECOMMEND_FOR_GOLD - 1),
                new User("madnite1", "이상호", "p4", Level.SILVER, 60, MIN_RECOMMEND_FOR_GOLD),
                new User("green", "오민규", "p5", Level.GOLD, 100, Integer.MAX_VALUE)
        );
    }

    @Test
    public void upgradeLevels() throws Exception {
        userDao.deleteAll();
        for (User user : this.users) {
            userDao.add(user);
        }

        MockUserDao mockUserDao = new MockUserDao(userDao.getAll());
        userServiceImpl.setUserDao(mockUserDao);


        // 테스트 대상 실행
        userServiceImpl.upgradeLevels();


//        // DB 결과 확인
//        checkLevelUpgraded(users.get(0), false);
//        checkLevelUpgraded(users.get(1), true);
//        checkLevelUpgraded(users.get(2), false);
//        checkLevelUpgraded(users.get(3), true);
//        checkLevelUpgraded(users.get(4), false);

        List<User> updated = mockUserDao.getUpdated();
        assert (updated.size() == 2);
        checkUserAndLevel(updated.get(0), "joytouch", Level.SILVER);
        checkUserAndLevel(updated.get(1), "madnite1", Level.GOLD);
    }

    @Test
    public void add() {
        userDao.deleteAll();

        User userWithLevel = users.get(4); // GOLD 레벨
        User userWithoutLevel = users.get(0);
        userWithoutLevel.setLevel(null);

        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        User userWithLevelRead = userDao.get(userWithLevel.getId());
        User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());

        assert (userWithLevelRead.getLevel().equals(userWithLevel.getLevel()));
        assert (userWithoutLevelRead.getLevel().equals(Level.BASIC));
    }

    @Test
    @DirtiesContext
    public void upgradeAllOrNothing() throws Exception {
        UserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(this.userDao);

//        ProxyFactoryBean txProxyFactoryBean = context.getBean("&userService", ProxyFactoryBean.class);
//        txProxyFactoryBean.setTarget(testUserService);
//        UserService txUserService = (UserService) txProxyFactoryBean.getObject();

        userDao.deleteAll();

        for (User user : users) userDao.add(user);
        try {
            testUserService.upgradeLevels();
            fail("TestUserService Exception expected");
        } catch (TestUserServiceException e) {

        }
        checkLevelUpgraded(users.get(3), false);
        userServiceImpl.setMailSender(mailSender);
    }

    private void checkLevelUpgraded(User user, boolean upgraded) {
        User userUpdate = userDao.get(user.getId());
        if (upgraded) {
            assert (userUpdate.getLevel().equals(user.getLevel().nextLevel()));
        } else {
            assert (userUpdate.getLevel().equals(user.getLevel()));
        }

    }

    private void checkUserAndLevel(User updated, String expectedId, Level expectedLevel) {
        assert (updated.getId().equals(expectedId));
        assert (updated.getLevel() == expectedLevel);
    }

    @Test
    public void mockUpgradeLevels() throws Exception {
        UserServiceImpl userServiceImpl = new UserServiceImpl();

        UserDao mockUserDao = mock(UserDao.class);
        when(mockUserDao.getAll()).thenReturn(this.users);
        userServiceImpl.setUserDao(mockUserDao);

        MailSender mockMailSender = mock(MailSender.class);
        userServiceImpl.setMailSender(mockMailSender);

        userServiceImpl.upgradeLevels();

        verify(mockUserDao, times(2)).update(any(User.class));
        verify(mockUserDao, times(2)).update(any(User.class));
        verify(mockUserDao).update(users.get(1));
        assert (users.get(1).getLevel().equals(Level.SILVER));
        verify(mockUserDao).update(users.get(3));
        assert (users.get(3).getLevel().equals(Level.GOLD));

        ArgumentCaptor<SimpleMailMessage> mailMessageArg = ArgumentCaptor.forClass(SimpleMailMessage.class);

    }

    @Test
    public void readOnlyTransactionAttribute() {
        assertThatThrownBy(() -> {
            testUserService.getAll();
        }).isInstanceOf(TransientDataAccessException.class);
    }

    // 이걸 이용해서 자동 프록시 생성기를 사용하게 해야하는데 옛날 방식이라서 실패
    static class TestUserService extends UserServiceImpl {
        private String id = "madnite1";

        private TestUserService(String id) {
            this.id = id;
        }

        public List<User> getAll() {
            for (User user : super.getAll()) {
                super.update(user);
            }
            return null;
        }

        protected void upgradeLevel(User user) {
            if (user.getId().equals(this.id)) throw new TestUserServiceException();
            super.upgradeLevel(user);
        }

    }

    static class TestUserServiceException extends RuntimeException {

    }

    static class MockUserDao implements UserDao {
        private final List<User> users;
        private final List<User> updated = new ArrayList();


        private MockUserDao(List<User> users) {
            this.users = users;
        }

        public List<User> getUpdated() {
            return this.updated;
        }


        public void update(User user) {
            updated.add(user);
        }


        public List<User> getAll() {
            return this.users;
        }


        // 아래로는 테스트에 사용되지 않는 메소드들
        public void deleteAll() {
            throw new UnsupportedOperationException();
        }

        public void add(User user) {
            throw new UnsupportedOperationException();
        }


        public int getCount() {
            throw new UnsupportedOperationException();
        }


        public User get(String id) {
            throw new UnsupportedOperationException();
        }

    }
}


