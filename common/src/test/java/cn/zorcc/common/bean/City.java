package cn.zorcc.common.bean;

import java.util.Objects;

public class City {
    private int population;
    private double area;
    private boolean capital;
    private char category;
    private long elevation;
    private String name;

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public boolean isCapital() {
        return capital;
    }

    public void setCapital(boolean capital) {
        this.capital = capital;
    }

    public char getCategory() {
        return category;
    }

    public void setCategory(char category) {
        this.category = category;
    }

    public long getElevation() {
        return elevation;
    }

    public void setElevation(long elevation) {
        this.elevation = elevation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return population == city.population && Double.compare(area, city.area) == 0 && capital == city.capital && category == city.category && elevation == city.elevation && Objects.equals(name, city.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(population, area, capital, category, elevation, name);
    }
}
