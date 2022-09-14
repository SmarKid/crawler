package cn.wust.yq.crawler.pojo;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Entity
@Table(name = "yq_info")
public class YQInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    @Column(name = "hbaseid")
    private String hbaseid;
    @Column(name = "title")
    private String title;
    @Column(name = "source")
    private String source;
    @Column(name = "release_time")
    private Date releaseTime;
    @Column(name = "author")
    private String author;
    @Column(name = "release_site")
    private String releaseSite;
    @Column(name = "attribute")
    private String attribute;
    @Column(name = "region")
    private String region;
    @Column(name = "key_word")
    private String keyWord;
    @Column(name = "source_link")
    private String sourceLink;
    @Column(name = "body_text")
    private String bodyText;
    @Column(name = "release_address")
    private String releaseAddress;

    public YQInfo() {
    }

    public YQInfo(String hbaseid, String title,
                  String source, Date releaseTime,
                  String author, String releaseSite,
                  String attribute, String region,
                  String keyWord, String sourceLink,
                  String bodyText, String releaseAddress) {
        this.hbaseid = hbaseid;
        this.title = title;
        this.source = source;
        this.releaseTime = releaseTime;
        this.author = author;
        this.releaseSite = releaseSite;
        this.attribute = attribute;
        this.region = region;
        this.keyWord = keyWord;
        this.sourceLink = sourceLink;
        this.bodyText = bodyText;
        this.releaseAddress = releaseAddress;
    }

    public YQInfo(String hbaseid, String title,
                  String source, LocalDateTime releaseTime,
                  String author, String releaseSite,
                  String attribute, String region,
                  String keyWord, String sourceLink,
                  String bodyText, String releaseAddress) {
        this.hbaseid = hbaseid;
        this.title = title;
        this.source = source;
        setReleaseTime(releaseTime);
        this.author = author;
        this.releaseSite = releaseSite;
        this.attribute = attribute;
        this.region = region;
        this.keyWord = keyWord;
        this.sourceLink = sourceLink;
        this.bodyText = bodyText;
        this.releaseAddress = releaseAddress;
    }

    public String getReleaseAddress() {
        return releaseAddress;
    }

    public void setReleaseAddress(String releaseAddress) {
        this.releaseAddress = releaseAddress;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHbaseid() {
        return hbaseid;
    }

    public void setHbaseid(String hbaseid) {
        this.hbaseid = hbaseid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Date getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(Date releaseTime) {
        this.releaseTime = releaseTime;
    }

    public void setReleaseTime(LocalDateTime releaseTime) {
        Date date = Date.from(releaseTime.atZone( ZoneId.systemDefault()).toInstant());
        this.releaseTime = date;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getReleaseSite() {
        return releaseSite;
    }

    public void setReleaseSite(String releaseSite) {
        this.releaseSite = releaseSite;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getKeyWord() {
        return keyWord;
    }

    public void setKeyWord(String keyWord) {
        this.keyWord = keyWord;
    }

    public String getSourceLink() {
        return sourceLink;
    }

    public void setSourceLink(String sourceLink) {
        this.sourceLink = sourceLink;
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }
}
