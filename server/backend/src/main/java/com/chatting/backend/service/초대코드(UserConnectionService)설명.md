# invite() 메서드 동작 흐름

1. 초대 대상 찾기
   - userService.getUser(inviteCode)로 inviteCode의 소유자(초대 대상)를 찾음
   - 대상 없으면 "Invalid invite code." 반환(실패).
2. 자기 초대 방지
   - if (partnerUserId.equals(inviterUserId)) → 자기 자신 초대면 실패("Can't self invite.")
3. 현재 상태 조회
   - getStatus(inviterUserId, partnerUserId)로 현재 두 사용자 간 상태를 가져옴.
   - 상태가 NONE(없음) 또는 DISCONNECTED(끊김)이라면: 초대자 username을 DB에서 가져오고 setStatus(..., PENDING) 호출로 PENDING 상태(초대 요청 생성) 저장
   - 상태가 ACCEPTED이면 이미 연결된 관계이므로 Already connected with ... 반환.
   - 상태가 PENDING이나 REJECTED이면 이미 초대 중이거나 거절된 상태이므로 "Already invited to ..." 반환.
4. DB 업데이트
   - setStatus()에서 정렬된 (min,max) 키로 UserConnectionEntity를 저장하여 DB에 반영.