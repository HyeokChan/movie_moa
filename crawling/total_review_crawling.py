import requests
from bs4 import BeautifulSoup
import pymysql
import pickle
from konlpy.tag import Okt
import tensorflow as tf
import numpy as np
import math
import re
#저장된 모델과 토큰 불러오기
model = tf.keras.models.load_model('train_model.h5')
with open('text.pickle', 'rb') as handle:
    text = pickle.load(handle)
    
okt = Okt()

#새로 입력받는 리뷰를 토큰화 시킨다.
def tokenize(doc):
    # norm은 정규화, stem은 근어로 표시하기를 나타냄
    return ['/'.join(t) for t in okt.pos(doc, norm=True, stem=True)]

#빈도수가 높은 500단어 저장
selected_words = [f[0] for f in text.vocab().most_common(500)]

def term_frequency(doc):
    return [doc.count(word) for word in selected_words]

#카테고리 키워드
prodCategorys  = ['감독',  '연출',  '인생', '각본',  '분위기', '디렉션', '디렉팅', '메시지', '시나리오', '해석' , '아이디어', '스케일', '여운', '표현', '액션','분위기', '예술', '조화']
actingCategorys = ['연기',  '주인공',  '감정', '주연', '조연' , '배우', '연기력', '표현', '매력','케미']
StoryCategorys = ['스토리', '감동', '공감', '슬퍼', '슬펐', '재미', '눈물', '내용', '구성', '지루' ,'결말', '꿀잼', '전개', '엔딩', '신선', '몰입', '오락', '이야기', '개연성', '구성', '이해', '집중', '소재', '슬프']
BeautyCategorys = [ '색감', '앵글', '영상미', '3d', '그래픽', '화려', '볼거리', '아이맥스', '실감', '명암', 'CG', '카메라']
OSTCategorys = ['노래', '연주', 'OST', '음악', '배경음', '음향']

#카테고리별로 리뷰를 저장하기위한 리스트
prodreview = list()
actingreview = list()
Storyreview = list()
Beautyreview = list()
OSTreview = list()

#현재 상영중인 영화의 아이디 긁어옴
def get_movieID():
    url = 'https://movie.naver.com/movie/running/current.nhn?view=list&tab=normal&order=reserve'
    resp = requests.get(url)
    namelist = {}
    html = BeautifulSoup(resp.content, 'html.parser')
    movieId_tags = html.find('ul', class_='lst_detail_t1').findAll('dl', class_='lst_dsc')
    for movieId_tag in movieId_tags:
        movieID = movieId_tag.find("a")
        movieNames =movieId_tag.find("a").getText()
        movieID = str(movieID)
        movieIDindex = movieID.find("code=")
        movieID = movieID[movieIDindex+5:movieIDindex+11].replace('"',"")
        namelist.setdefault(movieNames,movieID)
    return namelist
    

def get_movieID_daum():
    namelist_daum={}
    for i in range(10):
        current_page_num = 1 + i
        url = 'https://movie.daum.net/premovie/released?page='
        position = url.find('=')
        URL_with_page_num = url[: position + 1] + str(current_page_num) + '&opt=reserve'
        req = requests.get(URL_with_page_num)
        soup = BeautifulSoup(req.text, 'html.parser')
        
        movie_infos = soup.findAll("ul",class_="list_boxthumb")
        for movie_info in movie_infos:
            movie_links = movie_info.findAll("a","link_g")
            
        for movie_link in movie_links:
            movie_id = movie_link["href"]
            movie_id = movie_id[22:]
            movie_name = movie_link.text
            namelist_daum.setdefault(movie_name,movie_id)
    return namelist_daum

def get_data(movieId,movieIds):
    prodreview.clear()
    actingreview.clear()
    Storyreview.clear()
    Beautyreview.clear()
    OSTreview.clear()
    
    movieId_number = movieIds.get(movieId)
    first_url = 'https://movie.naver.com/movie/bi/mi/pointWriteFormList.nhn?code='
    second_url = '&type=after&page='
    movieIdposition = first_url.find('=')
    
    test_url = first_url[: movieIdposition+1] + movieId_number + second_url + '1' 
    resp = requests.get(test_url)
    html = BeautifulSoup(resp.content, 'html.parser')
    #전체 댓글 수
    try:
        result = html.find('div', {'class':'score_total'}).find('strong').findChildren('em')[0].getText()
        total_count = int(result.replace(',', ''))
        for i in range(1,int(total_count / 10) + 1):
            url = first_url[:movieIdposition+1] + str(movieId_number) + second_url + str(i)
            print(url)
            resp = requests.get(url)
            html = BeautifulSoup(resp.content, 'html.parser')
            score_result = html.find('div', {'class': 'score_result'})
            lis = score_result.findAll('li')
            for li in lis:
                review_text = li.find('p').getText()        
                review_text = review_text.replace("관람객", "").replace("  ", "").replace("\t", "").replace("\n\n", "").replace('\r', '').strip()               
                if('스포일러가 포함된 감상평입니다. 감상평 보기' in review_text):
                    pass
                elif('<span>' in review_text):
                    pass
                else:
                    for prodCategory in prodCategorys : 
                        if prodCategory in review_text:    
                            prodreview.append(review_text)
                            break;
                    for actingCategory in actingCategorys : 
                        if actingCategory in review_text:
                            actingreview.append(review_text)
                            break;
                    for StoryCategory in StoryCategorys : 
                        if StoryCategory in review_text:
                            Storyreview.append(review_text)
                            break;
                    for BeautyCategory in BeautyCategorys : 
                        if BeautyCategory in review_text:
                            Beautyreview.append(review_text)
                            break;
                    for OSTCategory in OSTCategorys : 
                        if OSTCategory in review_text:
                             OSTreview.append(review_text)
                             break;                                                                                                                          
    except Exception as e:
        print('Error', e)
        pass
        
    
def clean_text(review_text):
    review_text = re.sub('[a-zA-Z]', '', review_text)
    review_text = re.sub('[\{\}\[\]\/?.,;:|\)*~`!^\-_+<>@\#$%&\\\=\(\'\"]','', review_text)
    review_text = re.sub('모바일에서 더보기 클릭시','',review_text)
    review_text = review_text[50:]
    return review_text


def get_data_daum(movieId,movieIds_daum):
    URL = 'https://movie.daum.net/moviedb/grade?movieId='
    if movieId in movieIds_daum:
        review_text=''        
        URL_with_movie_id = URL + str(movieIds_daum.get(movieId)) + '&type=netizen'
        req_id = requests.get(URL_with_movie_id)
        soup_id = BeautifulSoup(req_id.text, 'html.parser')
        review_page = soup_id.find("span","txt_menu").getText()
        review_page = review_page.replace("(","")
        review_page = review_page.replace(")","")
        review_page = review_page.replace(",","")
        review_page = math.floor(int(review_page) / 10) + 1
        for i in range(1,review_page): # i는 0부터 page_num 전까지, ex) 10 -> 0 ~ 9 range(review_page)
            current_page_num = 1 + i
            URL_with_page_num = URL_with_movie_id + '&page=' + str(current_page_num)
            req = requests.get(URL_with_page_num)
            soup = BeautifulSoup(req.text, 'html.parser')
            reviews=soup.findAll("p",class_="desc_review")
            for review in reviews:
                review_text=str(review.find_all(text=True))
                review_text=clean_text(review_text)
                review_text=review_text.strip()
                if (review_text!=""):
                    for prodCategory in prodCategorys : 
                        if prodCategory in review_text:    
                            prodreview.append(review_text)
                            break;
                    for actingCategory in actingCategorys : 
                        if actingCategory in review_text:
                            actingreview.append(review_text)
                            break;
                    for StoryCategory in StoryCategorys : 
                        if StoryCategory in review_text:
                            Storyreview.append(review_text)
                            break;
                    for BeautyCategory in BeautyCategorys : 
                        if BeautyCategory in review_text:
                            Beautyreview.append(review_text)
                            break;
                    for OSTCategory in OSTCategorys : 
                        if OSTCategory in review_text:
                            OSTreview.append(review_text)
                            break;
                                
def predict_pos_neg(review):
    return_score=0
    token = tokenize(review)
    tf = term_frequency(token)
    data = np.expand_dims(np.asarray(tf).astype('float32'), axis=0)
    score = float(model.predict(data))
    if(score > 0.5):
        #print("[{}]는 {:d}% 확률로 긍정 리뷰이지 않을까 추측해봅니다.^^\n".format(review, int(score * 100)))
        return_score=int(score * 10)
        #print(return_score)
        
    else:
        #print("[{}]는 {:d}% 확률로 부정 리뷰이지 않을까 추측해봅니다.^^;\n".format(review, int((1 - score) * 100)))
        return_score=-int((1 - score) * 10)
        #print(return_score)
        
    return return_score

# 메인 함수
def main():
    movieIds = get_movieID()
    movieIds = {'신의 한 수: 귀수편':'179159', '82년생 김지영':'179482'}
    movieIds_daum = get_movieID_daum()
    movieIds_daum = {'신의 한 수: 귀수편':'124804', '82년생 김지영':'124806'}
    # MySQL Connection 연결
    conn = pymysql.connect(host='rnjsgur12.cafe24.com', user='rnjsgur12', password='rnjsgur12',db='rnjsgur12', charset='utf8')
    curs = conn.cursor()
    curs.execute("set names utf8")
    sql = "DELETE FROM review_count"
    curs.execute(sql)
    for movieId in movieIds:
        print(movieId)
        pro_score=0
        act_score=0
        sto_score=0
        bea_score=0
        ost_score=0
        get_data(movieId,movieIds)
        get_data_daum(movieId,movieIds_daum)
        for i in prodreview:
            pro_score = pro_score + predict_pos_neg(i)
        for i in actingreview:
            act_score = act_score + predict_pos_neg(i)
        for i in Storyreview:
            sto_score = sto_score + predict_pos_neg(i)
        for i in Beautyreview:
            bea_score = bea_score + predict_pos_neg(i)
        for i in OSTreview:
            ost_score = ost_score + predict_pos_neg(i)
            
        #print(movieIds.get(movieId))
        # SQL문 실행
        sql = "insert into review_count(id,name,pro,act,sto,bea,ost) values(%s,%s,%s,%s,%s,%s,%s)"
        curs.execute(sql,('',movieId, pro_score, act_score, sto_score, bea_score, ost_score))
        conn.commit()
    
    # Connection 닫기
    conn.close()
    
        

if __name__ == '__main__':
    main()