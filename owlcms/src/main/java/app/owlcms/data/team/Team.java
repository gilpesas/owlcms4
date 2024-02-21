/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.team;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.athlete.Gender;

/**
 * A non-persistent class to assist in creating reports and team results
 *
 * @author Jean-François Lamy
 */
public class Team {

	public static Comparator<Team> pointsComparator = ((a,
	        b) -> -ObjectUtils.compare(a.getPoints(), b.getPoints(), true));

	public static Comparator<Team> scoreComparator = ((a,
	        b) -> -ObjectUtils.compare(a.sinclairScore, b.sinclairScore, true));

	private int counted;

	private Gender gender;

	private String name;

	private int points = 0;

	private double sinclairScore = 0.0D;

	private long size;

	private double smfScore = 0.0D;

	private double robi = 0.0D;

	private double hsr;

	public Team(String curTeamName, Gender gender) {
		name = curTeamName;
		this.gender = gender;
	}

	public int getCounted() {
		return counted;
	}

	public Gender getGender() {
		return gender;
	}

	public String getName() {
		return name;
	}

	public int getPoints() {
		return points;
	}

	public double getRobi() {
		return robi;
	}

	public double getSinclairScore() {
		return sinclairScore;
	}

	public long getSize() {
		return size;
	}

	/**
	 * @return the smfScore
	 */
	public double getSmfScore() {
		return smfScore;
	}

	public void setCounted(int counted) {
		this.counted = counted;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public void setRobi(double robi) {
		this.robi = robi;
	}

	public void setSinclairScore(double d) {
		this.sinclairScore = d;
	}

	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * @param smfScore the smfScore to set
	 */
	public void setSmfScore(double smfScore) {
		this.smfScore = smfScore;
	}

	public double getHsr() {
		return hsr;
	}

	public void setHsr(double hsr) {
		this.hsr = hsr;
	}

}
