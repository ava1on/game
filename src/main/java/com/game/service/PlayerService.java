package com.game.service;

import com.game.controller.PlayerOrder;
import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.beans.FeatureDescriptor;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class PlayerService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Player> filterPlayers(Map<String, String> params, boolean withPaging) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Player> playerCriteria = cb.createQuery(Player.class);

        Root<Player> root = playerCriteria.from(Player.class);
        playerCriteria.select(root);
        List<Predicate> predicates = new ArrayList<>();

        if (params.containsKey("name")) {
            predicates.add(cb.like(root.get("name"), "%" + params.get("name") + "%"));
        }
        if (params.containsKey("title")) {
            predicates.add(cb.like(root.get("title"), "%" + params.get("title") + "%"));
        }
        if (params.containsKey("race")) {
            Expression<Race> p = null;
            predicates.add(cb.equal(root.get("race"), Race.valueOf(params.get("race"))));
        }
        if (params.containsKey("profession")) {
            predicates.add(cb.equal(root.get("profession"), Profession.valueOf(params.get("profession"))));
        }
        if (params.containsKey("after")) {
            long value = Long.parseLong(params.get("after"));
            predicates.add(cb.greaterThanOrEqualTo(root.get("birthday").as(Date.class), new Date(value)));
        }
        if (params.containsKey("before")) {
            long value = Long.parseLong(params.get("before"));
            predicates.add(cb.lessThanOrEqualTo(root.get("birthday").as(Date.class), new Date(value)));
        }
        if (params.containsKey("banned")) {
            predicates.add(cb.equal(root.get("banned"), Boolean.valueOf(params.get("banned"))));
        }
        if (params.containsKey("minExperience")) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("experience"),
                    Integer.parseInt(params.get("minExperience"))));
        }
        if (params.containsKey("maxExperience")) {
            predicates.add(cb.lessThanOrEqualTo(root.get("experience"),
                    Integer.parseInt(params.get("maxExperience"))));
        }
        if (params.containsKey("minLevel")) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("level"),
                    Integer.parseInt(params.get("minLevel"))));
        }
        if (params.containsKey("maxLevel")) {
            Expression<Integer> p = cb.parameter(Integer.class);
            predicates.add(cb.lessThanOrEqualTo(root.get("level"),
                    Integer.parseInt(params.get("maxLevel"))));
        }

        playerCriteria.where(predicates.toArray(new Predicate[]{}));

        playerCriteria.orderBy(cb.asc(root.get(PlayerOrder.valueOf(params.getOrDefault("order", "ID")).getFieldName())));

        int page = params.containsKey("pageNumber") ? Integer.parseInt(params.get("pageNumber")) : 0;
        int size = params.containsKey("pageSize") ? Integer.parseInt(params.get("pageSize")) : 3;

        TypedQuery<Player> q = entityManager.createQuery(playerCriteria);
        if (withPaging) {
            q.setFirstResult(page * size).setMaxResults(size);
        }

        return q.getResultList();
    }

    public void processEntity(Player player) {
        validatePlayer(player);
        calculateExperience(player);
    }

    public void validateId(Long id) {
        if (id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    public void copyValues(Player existingPlayer, Player player) {
        BeanUtils.copyProperties(player, existingPlayer, getIgnoreProperties(player));
    }

    private String[] getIgnoreProperties(Player player) {
        final BeanWrapper wrapper = new BeanWrapperImpl(player);

        Stream<String> nullValues = Stream.of(wrapper.getPropertyDescriptors())
                .map(FeatureDescriptor::getName)
                .filter(propertyName -> wrapper.getPropertyValue(propertyName) == null);

        return Stream.concat(nullValues, Stream.of("id", "level", "untilNextLevel"))
                .toArray(String[]::new);
    }

    private void validatePlayer(Player player) {
        if (player == null || !isNameValid(player) || !isTitleValid(player) || player.getRace() == null
                || player.getProfession() == null || !isDateValid(player) || !isExperienceValid(player)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isNameValid(Player player) {
        return !(player.getName() == null) && player.getName().length() <= 12 && player.getName().length() > 0;
    }

    private boolean isTitleValid(Player player) {
        return !(player.getTitle() == null) && player.getTitle().length() < 30;
    }

    private boolean isDateValid(Player player) {
        java.util.Date birthday = player.getBirthday();
        if (birthday == null) return false;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(birthday);
        int year = calendar.get(Calendar.YEAR);
        return year > 2000 && year < 3000;
    }

    private boolean isExperienceValid(Player player) {
        return !(player.getExperience() == null) && player.getExperience() > 0 && player.getExperience() < 10_000_001;
    }

    private void calculateExperience(Player player) {
        int level = (int) ((Math.sqrt(2500 + 200 * player.getExperience()) - 50) / 100);
        int untilNextLevel = 50 * (level + 1) * (level + 2) - player.getExperience();
        player.setLevel(level);
        player.setUntilNextLevel(untilNextLevel);
    }
}