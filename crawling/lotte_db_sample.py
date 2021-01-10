"""롯데시네마 크롤링"""
from bs4 import BeautifulSoup
import requests
import re
from selenium import webdriver
import time
from datetime import datetime, timedelta
import datetime
import pandas as pd
from pandas import DataFrame
import pymysql

options = webdriver.ChromeOptions()
#options.add_argument('headless')
options.add_argument('window-size=1920x1080')
options.add_argument("disable-gpu")
driver = webdriver.Chrome('C:/sele/chromedriver', chrome_options=options)

# 긁어 올 URL
CINEMAID_BEFORE_URL = 'http://www.lottecinema.co.kr/LCHS/Contents/Cinema/Cinema-Detail.aspx?cinemaID='
CINEMAIL_AFTER_URL = '&divisionCode=1&detailDivisionCode='

# 크롤링 함수
def get_text(URL, division_code_list,position_list):
    
    # MySQL Connection 연결
    conn = pymysql.connect(host='rnjsgur12.cafe24.com', user='rnjsgur12', 
                           password='rnjsgur12',db='rnjsgur12', charset='utf8')
    curs = conn.cursor()
    curs.execute("set names utf8")
    sql = "DELETE FROM lotte_movie"
    curs.execute(sql)
    
    division=0
    division_position=0    
    for cinema_id_list in division_code_list:
        division = division+1
        
        if(division==8):
            division=101
        
        area_position = 0
        for cinema_id in cinema_id_list:
            
            print(cinema_id)
            cinema_position = CINEMAID_BEFORE_URL.find('=') # index 그 문자가지고 있는 딱 맞는 인덱스자리 30
            URL_with_page_num = URL[: cinema_position + 1] + str(cinema_id) + CINEMAIL_AFTER_URL + str(division)
            print(URL_with_page_num)
            driver.get(URL_with_page_num)
            time.sleep(2)
            try:
                driver.find_element_by_xpath('//*[@id="pop_wrap01"]/div/div/a').click()
                time.sleep(2)
            except:
                pass
            for click in range(0,4):
                click_tag = '//*[@id="a_cont_cinema"]/div/div[3]/fieldset/div/label['+str(click+1)+']'
                driver.find_element_by_xpath(click_tag).click()
                time.sleep(2)
                html = driver.page_source
                soup = BeautifulSoup(html, 'html.parser')
                movies_info = soup.find("div","time_aType time"+str(cinema_id))
                movie_position = soup.find("h2","sub_tit").getText()
                real_area = soup.find("p","sub_addr2").get_text()
                real_area_position = real_area.find('\n')
                real_area = real_area[:real_area_position]
                first_date_str = soup.find("div","calendarArea").find("input")["value"]
                date_obj = datetime.datetime.strptime(first_date_str,"%Y-%m-%d").date()
                date_obj = date_obj + datetime.timedelta(days=click)
                try:
                    movies = movies_info.findAll("dl")
                    for movie in movies:
                        movie_name = movie.find("dt").get_text()
                        movie_name = movie_name[3:]         
                        movie_name = movie_name.replace("  ","").replace("\n","")                                    
                        movie_counts = movie.findAll("a")
                        movie_counts = movie_counts[1:]
                        for movie_count in movie_counts:
                            
                            #movie_area = movie_count.find("span","cineD2")
                            movie_area = movie_count.find("span","cineD2")
                            movie_area = movie_area.get_text()
                            movie_time = movie_count.find("span","clock")              
                            movie_time = movie_time.get_text().replace("조조","").replace("심야","")
                            movie_seat = movie_count.find("span","ppNum")              
                            movie_seat = movie_seat.get_text()
                            print(movie_name)
                            print(movie_position)
                            print(real_area)
                            print(date_obj)
                            print(movie_time)
                            print(movie_seat)
                            print(movie_area)
                            print("-----------------------------------------------")
                            
                            # SQL문 실행
                            sql = "insert into lotte_movie(id,movie_name,position_name,address,date,time,seat,tube,movie_cor,latitude,longitude) values(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)"
                            curs.execute(sql,('',movie_name, movie_position, real_area, date_obj, movie_time, movie_seat, movie_area,"lotte_image.jpg",position_list[division_position][area_position][0],position_list[division_position][area_position][1]))
                            conn.commit()
                
                except:
                    pass   
            area_position = area_position+1
        division_position = division_position+1
    # Connection 닫기
    conn.close()
        

# 메인 함수
def main():
    
# =============================================================================
#     #division_code_list=[['1013'],['3015'],['4002'],['6001'],['9067'],['7001'],['9013'],['2009']]
#     division_code_list=[['1013'],['3015'],['4002'],['6001'],['9067']]
# =============================================================================

    
    position_list=[
            [['37.477650','126.889203'],['37.561564','126.853255'],['37.536689','127.125537'],['37.538548','127.073171'],['37.563277','126.803100'],['37.654837','127.061049'],['37.469336','126.897031'],['37.516574','127.021736'],['37.481038','126.952161'],['37.676531','127.055668'],['37.635775','127.023765'],['37.508833','126.889463'],['37.483888','126.930157'],['37.564269','126.981552'],['37.516384','126.907752'],['37.532916','126.959934'],['37.513958','127.105027'],['37.637534','126.917853'],['37.571903','127.072343'],['37.581014','127.048081'],['37.550279','126.913543'],['37.557210','126.924997'],['37.570916','127.021328']],
            [['37.591412','126.646606'],['37.286382','127.055488'],['37.479424','126.855278'],['37.423939','126.884480'],['37.409524','127.261230'],['37.611791','127.140749'],['37.661375','126.768753'],['37.650304','127.309169'],['37.213744','127.042589'],['37.502805','126.773124'],['37.485515','126.782322'],['37.493485','126.726522'],['37.489958','126.723301'],['37.357800','126.930630'],['37.274891','126.954444'],['37.431309','127.129595'],['37.441259','127.146440'],['37.318006','126.846224'],['37.067234','127.060873'],['37.264261','126.997392'],['37.313002','127.081073'],['37.351314','126.741778'],['37.318026','126.846265'],['37.308756','126.828913'],['37.007962','127.199511'],['37.402208','126.922344'],['37.399596','126.920203'],['37.795647','127.078911'],['37.489788','126.561282'],['37.146097','127.070649'],['37.274583','127.116444'],['37.235789','127.188566'],['37.472590','127.141280'],['37.744073','127.096709'],['37.400263','126.976136'],['37.546128','126.667461'],['37.441898','126.701377'],['37.670830','126.760983'],['37.712015','127.186646'],['37.718082','126.692589'],['37.729560','126.736751'],['37.390452','126.950654'],['36.995612','127.112464'],['37.115175','126.912475']],
            [['36.340980','127.389502'],['36.340643','127.389546'],['36.361947','127.378961'],['36.361942','127.378964'],['36.351954','127.379510'],['36.778756','126.465614'],['36.644636','127.421373'],['36.784865','127.016099'],['36.635754','127.489397'],['36.606991','127.503096'],['36.961699','127.898193']],
            [['35.154740','126.912278'],['35.154538','126.912264'],['35.159935','126.809631'],['35.961664','126.704825'],['35.976409','126.738212'],['35.190327','126.820150'],['35.949672','126.939873'],['35.834475','127.121836'],['35.796261','127.132700'],['35.148217','126.914697'],['35.834566','128.733400']],
            [['36.129278','128.334785'],['35.841621','129.213222'],['36.126653','128.336072'],['36.104267','128.385048'],['35.855734','128.550703'],['35.867828','128.694071'],['35.693275','128.456316'],['35.870245','128.596999'],['35.816659','128.539333'],['35.854478','128.507830'],['36.827226','128.617931'],['36.036661','129.362923'],['36.129103','128.334750'],['35.870966','128.590419'],['36.566143','128.699980'],['35.942227','128.563926']],
            [['37.330761','127.948855'],['37.522281','129.115418'],['37.335301','127.929846']],
            [['33.247499', '126.509251'],['33.520678', '126.588112'],['33.484067', '126.535807']],
            [['35.098316','129.036829'],['35.225948','128.884238'],['35.185141','128.829934'],['35.098664','129.029454'],['35.212578','129.077624'],['35.191777','129.213512'],['35.238543','128.583350'],['35.156855','129.056648'],['35.163244','128.983816'],['35.157165','129.063128'],['35.169652','129.131157'],['35.163834','128.107753'],['35.228305','129.089771'],['35.538192','129.338180'],['35.553162','129.320165'],['35.180369','128.139803'],['35.158892','128.697377'],['35.158903','128.697361'],['34.859330','128.430621'],['35.180802','128.559773'],['35.192848','128.082619'],['35.169180','129.176631']]            
            ]
    
    division_code_list=[['1013','1018','9010','1004','1009','1003','1017','9056','1012','1019','1022','1015','1007','1001','1002','1014','1016','1021','9053','1008','1010','1005','1011'],
                        ['3015','3030','3027','3025','3020','3026','3002','3021','3017','3011','9054','3003','3008','3031','3043','9009','3041','3012','3029','3024','3044','3016','3004','3028','3022','3007','3032','9063','9077','9060','3039','3040','3037','3033','3023','3035','3038','3013','3010','3014','3034','3018','9075','3036'],
                        ['4002','4006','4008','9044','4004','4005','4003','4007','9078'],
                        ['6001','9065','6007','6009','6004','9070','6002','6006','9047'],
                        ['5008','9050','9001','5013','5012','5006','9076','5005','5016','5004','9064','5007','9067','9066','9074','9057'],
                        ['7001','7002','7003'],
                        ['9013','9068','9071'],
                        ['2009','5015','5011','2012','2007','2010','9042','2004','2005','2008','2006','9069','2011','5001','5014','5017','5009','5002','9036','9072','9003','9059']]
    
    get_text(CINEMAID_BEFORE_URL,division_code_list,position_list)
    

if __name__ == '__main__':
    main()